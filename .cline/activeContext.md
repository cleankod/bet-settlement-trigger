# Active Context — Bet Settlement Trigger

## Current State

- Branch: `integration-tests`
- Based on: freshly merged `master` (settlement-adapter merged)
- Task: Integration tests complete — all 9 tests pass (3 REST validation + 4 end-to-end flow + 2 BetTest unit tests)

## Most Recent Decisions

- **Two-type Bet model**: `UnsavedBet` (no id, before persistence) + `Bet` (primitive `long id`, guaranteed by type)
- **BetStatus**: `PENDING`, `WON`, `LOST`
- **Bet.settle()** throws `BetAlreadySettledException` if not PENDING
- **BetSettlement** is dual-purpose (published message + inbound command); WON/LOST not in command — derived by `Bet.settle()` only
- **BetRepository**: `save(UnsavedBet)` → `Bet`; `save(Bet)` → `void`; `findById(long)`; `findPendingByEventId(String)`
- **Single writer**: `BetSettlementService` is the only component that calls `save(Bet)`
- **EventOutcome**: `eventId`, `eventName`, `eventWinnerId` — `eventName` is carried through to Kafka for audit/human-readable enrichment; no domain invariant references it
- **BetEntity**: record with `@Id Long id`, factory methods, `toDomain()`
- **SpringDataBetRepository**: package-private, `@Query` for pending-by-event
- **JdbcBetRepositoryAdapter**: `@Repository`, maps entity ↔ domain
- **V1 migration**: `src/main/resources/db/migration/V1__create_bets.sql`
- **V2 seed migration**: `src/test/resources/db/migration/` — distinct event IDs per scenario
- **Groovy DSL** confirmed; **JUnit 5** confirmed
- **KafkaTopics.EVENT_OUTCOMES**: shared constant; topic name not duplicated
- **EventOutcomePublicationException**: lives in `application.port.out` (transport concern, not domain invariant)
- **Kafka publish timeout**: configurable via `app.kafka.publish-timeout` (default `5s`)
- **LoggingBetSettlementPublisher**: mock RocketMQ — logs JSON
- **LocalBetSettlementPublisher**: `@Profile("local")` — wires publisher callback to handler for full end-to-end flow without RocketMQ
- **spring-boot-starter-kafka** (not bare `spring-kafka`) required for `@KafkaListener` consumer auto-configuration
- **spring-boot-starter-flyway** (not bare `flyway-core`) required for Flyway autoconfiguration
- **H2 URL**: must include `CASE_INSENSITIVE_IDENTIFIERS=TRUE` for Flyway compat
- **Kafka consumer**: `spring.json.value.default.type` required for reliable JsonDeserializer type inference

## Branching Workflow

1. User creates branch, tells Cline.
2. Cline implements with individual commits.
3. User pushes, reviews, may request fixes.
4. User merges to `master`, pulls, tells Cline.
5. Cline acknowledges, suggests next branch name.
6. Repeat.

## Immediate Next Steps After This Branch Merges

1. `documentation` → README (quick start, API examples, architecture, trade-offs, AI disclosure), MkDocs internal docs
2. `observability` → Micrometer metrics (bets settled, events received), MDC correlation ID in all log paths

## Session Resumption Notes

- When resuming, read `progress.md` for overall status and `activeContext.md` for recent decisions.
- Always check `git branch --show-current` and `git status` before making changes.
- Do not start the next branch until the user confirms master is freshly pulled.
- Build command: `JAVA_HOME=~/.sdkman/candidates/java/current ./gradlew compileJava`
- Test command: `JAVA_HOME=~/.sdkman/candidates/java/current ./gradlew test`
- Docker must be running for Testcontainers tests to work
