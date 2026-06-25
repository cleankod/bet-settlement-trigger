# Active Context — Bet Settlement Trigger

## Current State

- Branch: `documentation`
- Based on: freshly merged `master` (integration-tests merged)
- Task: Documentation complete — README + MkDocs (5 pages) written and committed

## Most Recent Decisions

- **Two-type Bet model**: `UnsavedBet` (no id, before persistence) + `Bet` (primitive `long id`, guaranteed by type)
- **BetStatus**: `PENDING`, `WON`, `LOST`
- **Bet.settle()** throws `BetAlreadySettledException` if not PENDING
- **BetSettlement** is dual-purpose (published message + inbound command); WON/LOST not in command — derived by `Bet.settle()` only
- **BetRepository**: `save(UnsavedBet)` → `Bet`; `save(Bet)` → `void`; `findById(long)`; `findPendingByEventId(String)`
- **Single writer**: `BetSettlementService` is the only component that calls `save(Bet)`
- **EventOutcome**: `eventId`, `eventName`, `eventWinnerId` — `eventName` is carried through to Kafka for audit/human-readable enrichment
- **BetEntity**: record with `@Id Long id`, factory methods, `toDomain()`
- **SpringDataBetRepository**: package-private, `@Query` for pending-by-event
- **JdbcBetRepositoryAdapter**: `@Repository`, maps entity ↔ domain
- **V1 migration**: `src/main/resources/db/migration/V1__create_bets.sql`
- **V2 seed migration**: `src/test/resources/db/migration/` — distinct event IDs per scenario
- **spring-boot-starter-kafka** (not bare `spring-kafka`) required for `@KafkaListener` consumer auto-configuration
- **spring-boot-starter-flyway** (not bare `flyway-core`) required for Flyway autoconfiguration
- **H2 URL**: must include `CASE_INSENSITIVE_IDENTIFIERS=TRUE` for Flyway compat
- **Kafka consumer**: `JacksonJsonDeserializer` + `spring.json.java-type-info.default-type` (Jackson 3)
- **MkDocs**: builds with `python3.12` via `venv`; `python3.14` (Homebrew) has broken `pyexpat`

## Branching Workflow

1. User creates branch, tells Cline.
2. Cline implements with individual commits.
3. User pushes, reviews, may request fixes.
4. User merges to `master`, pulls, tells Cline.
5. Cline acknowledges, suggests next branch name.
6. Repeat.

## Project Status

`documentation` is the final branch. After merge, the project is complete as scoped.
Observability and all other future improvements are documented in README "Given More Time".

## Session Resumption Notes

- When resuming, read `progress.md` for overall status and `activeContext.md` for recent decisions.
- Always check `git branch --show-current` and `git status` before making changes.
- Do not start the next branch until the user confirms master is freshly pulled.
- Build command: `./gradlew compileJava`
- Test command: `./gradlew test`
- MkDocs command: `source venv/bin/activate && mkdocs serve` (use python3.12, not 3.14)
- Docker must be running for Testcontainers tests to work
