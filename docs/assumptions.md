# Open Questions and Assumptions

This page documents known simplifications made during implementation and the reasoning behind them.
Each item describes the real-world behaviour, what the assignment specifies, and what would
need to change to make the service production-correct.

---

## Event Market ID and multi-market settlement

### Real-world behaviour

In a real sports betting platform, a single event (e.g. a football match) can have many **markets** —
each market is an independent betting question:

- *Match winner* — who wins the match?
- *First goal scorer* — who scores first?
- *Total goals over/under 2.5* — how many goals are scored?

Each market has its own set of possible **selections** and its own **winner** when the event
concludes. A bet on the "first goal scorer" market settles against a different winner than a bet
on the "match winner" market. When an event concludes, multiple settlement messages are typically
emitted — one per resolved market.

The event outcome message in a real system would therefore carry a **market reference**:

```json
{
  "eventId": "match-101",
  "marketId": "market-first-goal-scorer",
  "eventWinnerId": "player-jones"
}
```

Bets would be matched by **both `eventId` and `marketId`**, and only bets in that specific market
would be settled against that market's winner.

### What the assignment specifies

The event outcome model in the assignment is:

```json
{
  "eventId": "...",
  "eventName": "...",
  "eventWinnerId": "..."
}
```

There is no `marketId` in the event outcome. The matching requirement says:

> "The System checks if we have bets in our database that can be settled **based on Event ID**
> from the event outcome message."

### How this service handles it

Matching is by `eventId` only. When an event outcome arrives, **all pending bets for that event
are settled against the single `eventWinnerId`**, regardless of their `eventMarketId`.

The `eventMarketId` field on the `Bet` is present because the assignment spec includes it as a
bet field. It is stored and returned, but plays no role in settlement logic today.

This is why the integration test helper `placeBet()` uses a constant `ANY_MARKET` — all values
behave identically.

### What would need to change

To make this production-correct:

1. Add `marketId` to `EventOutcome` (and the Kafka message)
2. Add `marketId` to the event outcome REST request
3. Change `findPendingByEventId` to `findPendingByEventAndMarket(eventId, marketId)`
4. Emit separate outcome messages per market (or support a multi-market outcome payload)
5. Update integration tests to place bets in specific markets and assert settlement only fires
   for the matching market

This is documented as a future improvement.

---

## "Event Winner ID" on a bet = the user's selected winner

### Assumption

The `eventWinnerId` field in the PDF's bet model is interpreted as the participant the user
**selected to win** (named `selectedWinnerId` in the code). The actual event winner arrives
later in the event outcome message.

### Why this interpretation

Without this interpretation, settlement logic cannot work: you need to compare what the user
predicted (`selectedWinnerId`) against what actually happened (`eventWinnerId` from the outcome).

### Alternative interpretation

The field could also mean the winner stored on the bet *after* settlement — but that would
duplicate data already present in the `status` field.

### Production consideration

A production data model would likely separate these cleanly: `selectedParticipantId` on the bet
and `actualWinnerId` or `resolvedParticipantId` on the settlement record.

---

## Settlement outcomes: WON or LOST only

### Assumption

A bet transitions from `PENDING` to either `WON` or `LOST`. No other terminal states are modelled.

### Real-world behaviour

Production systems typically support additional states:

- **VOID** — event cancelled before it concluded; stakes returned
- **PUSH** — draw on a market that doesn't offer a draw outcome; stakes returned
- **CASH_OUT** — user exits the bet early for a partial payout
- **SUSPENDED** — bet temporarily frozen pending review

### Why WON/LOST only

The assignment settlement logic is "if selected winner equals actual winner → WON, else → LOST".
No other outcome types are mentioned.

---

## One event outcome = all bets for that event resolved simultaneously

### Assumption

A single `POST /api/v1/event-outcomes` call for a given `eventId` immediately triggers settlement
for all pending bets on that event.

### Real-world behaviour

Different markets within an event resolve at different times. "First goal scorer" resolves
mid-game; "match winner" resolves at full-time; "player of the match" resolves after the ceremony.
Settlement would be triggered per-market as each resolves, not once for the whole event.

### Production consideration

Addressed by adding `marketId` to the event outcome — see the multi-market section above.

---

## Bet placement has no business validation beyond field presence

### Assumption

`POST /api/v1/bets` validates that all fields are present and `betAmount > 0`. No further
business rules are checked.

### What production would check

- The event exists and is accepting bets (not started, not cancelled)
- The selected winner is a valid participant in the specified market
- The user exists and has sufficient account balance
- Betting limits (minimum/maximum stake) are respected
- The market is still open (not suspended or closed)

### Why skipped

These validations require additional services (event catalogue, user accounts, wallet) that are
out of scope for the assignment.

---

## In-memory database: H2 vs. a plain Map

### Ambiguity

The assignment says "use an in-memory database for the bets." This phrase is intentionally
open-ended and in recruitment exercises it commonly means one of two things:

1. **A plain `ConcurrentHashMap`-based repository** — quick to write, no dependencies, commonly
   seen in home assignments to keep setup minimal
2. **An embedded relational database** (H2, HSQLDB) — still in-memory, but provides real SQL,
   schema migrations, and a persistence layer that looks like production

### Decision

We chose **H2 with Spring Data JDBC and Flyway migrations** for the following reasons:

- The `BetRepository` port and the `JdbcBetRepositoryAdapter` demonstrate how the architecture
  cleanly separates domain from persistence — regardless of whether the backend is a Map or a DB
- Flyway migrations (`V1__create_bets.sql`) show schema evolution discipline
- Swapping H2 for PostgreSQL or MySQL requires only a driver change and a connection string;
  all other code stays the same

### Limitation (same in both approaches)

All bet data is lost on application restart. This is expected and acceptable per the assignment.

---

## Concurrency and atomicity gaps

The service is single-consumer and single-instance in the assignment context. Three concurrency
hazards exist that are safe in this setup but would need to be addressed before scaling.

### B1 — TOCTOU race in BetSettlementService (partially mitigated)

`settle()` performs a read-check-write sequence: read the bet, check if PENDING, then write the
settled state. Without a transaction, two concurrent settlement commands for the same bet can
both read PENDING and both write. This is mitigated by `@Transactional` on the method, which
makes the read-check-write atomic against the database within a single connection. However, for
full safety under high concurrency, **optimistic locking** (`@Version` column) or a
`SELECT ... FOR UPDATE` / `UPDATE ... WHERE status = 'PENDING'` pattern would be required to
prevent concurrent writes even across transactions.

In the current single-consumer setup, duplicate settlement commands carry the same `eventWinnerId`,
so both writes produce the same final status — correctness holds accidentally.

### B2 — Partial-publish gap in EventOutcomeHandlingService

```
findPendingByEventId(eventId)
  → for each bet: publish(settlement command)  ← no transaction
```

If the publisher throws on bet N, bets 0..N-1 have settlement commands in flight and bets N..end
do not. On Kafka redelivery all pending bets are reprocessed — the first group's settlement is
idempotent, so eventual correctness holds. But the gap is real: the DB read and the publish are
not atomic. The production fix is the **transactional outbox pattern**: write settlement commands
to an outbox table in the same transaction as the status read, then relay them asynchronously.

### B3 — Concurrent consumers can double-publish settlement batches

If two consumer instances (or the same message redelivered) execute `findPendingByEventId`
simultaneously before any bets are settled, both read the same pending bets and both publish
settlement commands. `BetSettlementService` handles this idempotently, but N² settlement commands
are generated. In practice this multiplies message volume without causing data errors.

The fix is either a single-partition assignment (one consumer for a given `eventId`) or
deduplication keys on the settlement commands.
