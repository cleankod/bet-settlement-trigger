# Progress — Bet Settlement Trigger

## Completed Stages

| Branch | Content | Status |
|--------|---------|--------|
| `init-project-structure` | Gradle scaffold: `build.gradle`, `settings.gradle`, `gradle/libs.versions.toml`, `BetSettlementTriggerApplication.java`, `application.yml` | ✅ merged |
| `contenerized-local-runtime` | `Dockerfile` (multi-stage), `docker-compose.yml` (Kafka KRaft + RocketMQ) | ✅ merged |
| `gitignore-hexagonal-arch-fix` | Fixed `.gitignore`: `out/` → `/out/` to allow `application/port/out/` to be tracked | ✅ merged |
| `project-rules` | `.clinerules` with full project rules, commit conventions, branching workflow | ✅ merged |
| `project-memory-bank` | This memory bank | 🔄 in progress |

## In-Progress / Next Stages

| Branch | Content |
|--------|---------|
| `domain-model` | `BetStatus`, `Bet`, `EventOutcome`, `BetSettlement` |
| `application-ports-and-services` | port/in interfaces, application services |
| `in-memory-bet-persistence` | V1 migration, `BetEntity`, `JdbcBetRepositoryAdapter` |
| `event-outcome-rest-api` | REST controller, validation, 202 Accepted, error handling, correlation ID |
| `kafka-integration` | Kafka producer + consumer adapters |
| `settlement-adapter` | Mock settlement publisher, message handler, local simulator |
| `observability` | Business metrics, structured logging, MDC |
| `integration-tests` | Testcontainers Kafka tests, V2 seed migration, WON/LOST/idempotency tests |
| `documentation` | README, MkDocs, AI usage disclosure |

## Untracked Files Needing Attention

The following files exist on disk but are not yet committed (will be committed on their respective branches):

- `src/main/java/eu/cleankod/settlementtrigger/application/port/out/BetRepository.java`
- `src/main/java/eu/cleankod/settlementtrigger/application/port/out/BetSettlementPublisher.java`
- `src/main/java/eu/cleankod/settlementtrigger/application/port/out/EventOutcomePublisher.java`

These will be committed as part of the `application-ports-and-services` branch.
