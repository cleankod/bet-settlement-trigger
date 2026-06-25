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

## In-memory database: no durability across restarts

### Assumption

H2 runs in-memory — all bets are lost on application restart. This is explicitly required by the
assignment ("use an in-memory database for the bets").

### Production consideration

A persistent database (PostgreSQL, MySQL) with Flyway migrations would replace H2. The Flyway
migration files (`V1__create_bets.sql`) are already structured to work with any SQL database
with minimal changes.
