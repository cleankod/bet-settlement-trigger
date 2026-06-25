# Active Context — Bet Settlement Trigger

## Current State

- Branch: `in-memory-persistence`
- Based on: freshly merged `master` (application ports and services merged)
- Task: in-memory bet persistence implemented, ready to commit

## Most Recent Decisions

- **Two-type Bet model**: `UnsavedBet` (no id, before persistence) + `Bet` (primitive `long id`, guaranteed by type)
- **BetStatus**: `PENDING`, `WON`, `LOST`
- **Bet.settle()** throws `BetAlreadySettledException` if not PENDING
- **BetSettlement** is dual-purpose (published message + inbound command); WON/LOST not in command — derived by `Bet.settle()` only
- **BetRepository**: `save(UnsavedBet)` → `Bet`; `save(Bet)` → `void`; `findById(long)`; `findPendingByEventId(String)`
- **Single writer**: `BetSettlementService` is the only component that calls `save(Bet)`
- **EventOutcome**: only `eventId` + `eventWinnerId` (eventName lives in REST DTO only)
- **BetEntity**: record with `@Id Long id`, factory methods, `toDomain()`
- **SpringDataBetRepository**: package-private, `@Query` for pending-by-event
- **JdbcBetRepositoryAdapter**: `@Repository`, maps entity ↔ domain
- **V1 migration**: `src/main/resources/db/migration/V1__create_bets.sql`
- **V2 seed migration**: `src/test/resources/db/migration/` (created in test branch)
- **Groovy DSL** confirmed; **JUnit 5** confirmed

## Branching Workflow

1. User creates branch, tells Cline.
2. Cline implements with individual commits.
3. User pushes, reviews, may request fixes.
4. User merges to `master`, pulls, tells Cline.
5. Cline acknowledges, suggests next branch name.
6. Repeat.

## Immediate Next Steps After This Branch Merges

1. `event-outcome-rest-api` → `EventOutcomeRequest` (validated DTO), `EventOutcomeController` (POST /api/v1/event-outcomes → 202), global error handler, correlation ID filter

## Session Resumption Notes

- When resuming, read `progress.md` for overall status and `activeContext.md` for recent decisions.
- Always check `git branch --show-current` and `git status` before making changes.
- Do not start the next branch until the user confirms master is freshly pulled.
- Build command: `JAVA_HOME=~/.sdkman/candidates/java/current ./gradlew compileJava`
