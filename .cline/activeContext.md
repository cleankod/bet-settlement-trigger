# Active Context — Bet Settlement Trigger

## Current State

- Branch: `project-memory-bank`
- Based on: freshly merged `master` (includes gitignore fix + .clinerules)
- Task: creating memory bank files

## Most Recent Decisions

- **BetStatus**: `PENDING`, `WON`, `LOST` (not `PENDING/SETTLED`)
- **Bet.settle()** logic: `selectedWinnerId == eventWinnerId` → `WON`, otherwise `LOST`
- **Bet fields**: `id`, `userId`, `eventId`, `eventMarketId`, `selectedWinnerId`, `betAmount`, `status`
  - `eventMarketId` and `betAmount` added to match the PDF assignment requirements
- **V2 seed migration**: goes in `src/test/resources/db/migration/` (not src/main)
- **port/out gitignore**: fixed — `.gitignore` `out/` → `/out/` (root-only)
- **Groovy DSL** confirmed (not Kotlin)
- **JUnit 5** confirmed (not JUnit 6 as one version of rules mistakenly stated)
- **Branching**: one feature branch per stage, reviewed + merged via PR; no direct commits to master

## Branching Workflow

1. User creates branch, tells Cline.
2. Cline implements with individual commits.
3. User pushes, reviews, may request fixes.
4. User merges to `master`, pulls, tells Cline.
5. Cline acknowledges, suggests next branch name.
6. Repeat.

## What's On Disk But Not Yet Committed

These 3 files exist untracked and will be committed on the `application-ports-and-services` branch:

- `src/main/java/eu/cleankod/settlementtrigger/application/port/out/BetRepository.java`
- `src/main/java/eu/cleankod/settlementtrigger/application/port/out/BetSettlementPublisher.java`
- `src/main/java/eu/cleankod/settlementtrigger/application/port/out/EventOutcomePublisher.java`

## Immediate Next Steps After This Branch Merges

1. `domain-model` branch → `BetStatus`, `Bet`, `EventOutcome`, `BetSettlement`
2. `application-ports-and-services` → port/in interfaces + the 3 untracked port/out files + services
3. Continue per `progress.md`

## Session Resumption Notes

- When resuming, read `progress.md` for overall status and `activeContext.md` for recent decisions.
- Always check `git branch --show-current` and `git status` before making changes.
- The 3 untracked `port/out` files are intentionally left uncommitted until their proper branch.
- Do not start the next branch until the user confirms master is freshly pulled.
