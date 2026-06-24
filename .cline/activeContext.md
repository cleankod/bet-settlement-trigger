# Active Context — Bet Settlement Trigger

## Current State

- Branch: `application-ports-and-services`
- Based on: freshly merged `master` (domain model merged)
- Task: application ports and services created, ready to commit

## Most Recent Decisions

- **Two-type Bet model**: `UnsavedBet` (no id, before persistence) + `Bet` (primitive `long id`, guaranteed by type)
- **BetStatus**: `PENDING`, `WON`, `LOST`
- **Bet.settle()** throws `BetAlreadySettledException` if not PENDING
- **BetSettlement.of()** asserts event id equality, throws `EventMismatchException`
- **BetRepository**: `save(UnsavedBet)` → returns `Bet`; `save(Bet)` → returns `Bet`; `findById(long)`; `findPendingByEventId(String)`
- **EventOutcome**: only `eventId` + `eventWinnerId` (eventName removed from domain — lives in REST DTO only)
- **V2 seed migration**: `src/test/resources/db/migration/`
- **Groovy DSL** confirmed; **JUnit 5** confirmed
- **Domain exception rule**: no generic JDK exceptions for domain violations; no try/catch inside domain methods

## Branching Workflow

1. User creates branch, tells Cline.
2. Cline implements with individual commits.
3. User pushes, reviews, may request fixes.
4. User merges to `master`, pulls, tells Cline.
5. Cline acknowledges, suggests next branch name.
6. Repeat.

## Immediate Next Steps After This Branch Merges

1. `in-memory-bet-persistence` → `BetEntity`, `JdbcBetRepositoryAdapter`, `BetSettlementTriggerApplication.java`, `application.yml`, V1 migration

## Session Resumption Notes

- When resuming, read `progress.md` for overall status and `activeContext.md` for recent decisions.
- Always check `git branch --show-current` and `git status` before making changes.
- Do not start the next branch until the user confirms master is freshly pulled.
