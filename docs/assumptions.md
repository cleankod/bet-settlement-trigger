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
