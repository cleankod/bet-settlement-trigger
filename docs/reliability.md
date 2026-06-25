# Reliability

## Delivery guarantee: at-least-once

The service operates under an **at-least-once** delivery model for both Kafka and the
settlement messaging path.

- Kafka consumer uses `auto-offset-reset=earliest` and commits offsets after processing
- A message may be redelivered if the consumer restarts before committing its offset
- The service is designed to handle duplicate deliveries safely through idempotency

**Exactly-once semantics are not implemented.** This would require a transactional outbox
pattern for Kafka publishing and idempotency keys on settlement commands. These are documented
as future improvements.

## Idempotent settlement

Settlement is safe to re-execute at two independent layers:

### Layer 1 — Event outcome handling

`EventOutcomeHandlingService.handle()` calls `BetRepository.findPendingByEventId()`, which
returns only bets in `PENDING` status.

If the same event outcome is delivered twice:

- First delivery: bets are found as PENDING → settlement commands published → bets settled
- Second delivery: same bets are now WON or LOST → `findPendingByEventId` returns empty list → no settlement commands published → no-op

### Layer 2 — Bet settlement

`BetSettlementService.settle()` checks the bet status before settling:

- If the bet is `PENDING`: settle and save
- If the bet is already `WON` or `LOST`: log and skip — the duplicate is silently ignored

This second layer protects against duplicate settlement commands arriving from the messaging
layer (even if the first layer allows them through).

## Kafka error handling

The default Spring Kafka `DefaultErrorHandler` is used. If a message causes an exception,
it is retried with exponential backoff. A message that continues to fail indefinitely will
block the partition.

A `DeadLetterPublishingRecoverer` with a fixed retry limit is a future improvement.

## RocketMQ mock

`LoggingBetSettlementPublisher` logs settlement commands as JSON instead of publishing to a
real RocketMQ broker. This means:

- Settlement commands are **not durable** — they exist only as log entries
- No consumer group, no replay, no DLQ
- Under the `local` profile, `LocalBetSettlementPublisher` calls `SettleBetUseCase` directly,
  making the full end-to-end flow work in development and integration tests without a real broker

See [Decisions](decisions.md) for the rationale behind this trade-off.

## Kafka publish timeout

The Kafka producer is configured to block for a bounded time:

```yaml
app:
  kafka:
    publish-timeout: '5s'
```

`KafkaTemplate.send(...).get(timeout)` makes the publish synchronous and bounded.
If Kafka is unavailable or the publish times out, the REST endpoint returns `500 Internal Server Error`.
This ensures the caller knows the outcome was not accepted rather than silently lost.

## Database

H2 runs in-memory. The schema is managed by Flyway:

- `V1__create_bets.sql` — creates the `bets` table (production)
- `V2__seed_sample_bets.sql` — seeds test data (test classpath only, not in production)

On restart, all bet state is lost. This is acceptable for the assignment scope.
A persistent database is documented as a natural next step.
