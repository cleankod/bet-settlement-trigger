# Technical Context — Bet Settlement Trigger

## Tech Stack

| Concern | Technology |
|---------|-----------|
| Language | Java 25 |
| Framework | Spring Boot 4 |
| Build | Gradle Groovy DSL + version catalog (`gradle/libs.versions.toml`) |
| Persistence | Spring Data JDBC + H2 in-memory |
| Migrations | Flyway |
| Messaging (inbound) | Kafka (KRaft, no ZooKeeper) |
| Messaging (outbound) | RocketMQ — mocked via `LoggingBetSettlementPublisher` |
| Testing | JUnit 5, AssertJ, Testcontainers |
| Observability | SLF4J + Logback, Micrometer, Spring Boot Actuator |
| Docs | MkDocs |

## Architecture

Lightweight Hexagonal Architecture (Ports and Adapters).

```
eu.cleankod.settlementtrigger
  domain                         -- pure Java, no framework deps
  application
    port.in                      -- inbound use-case interfaces
    port.out                     -- outbound repository/publisher interfaces
    service                      -- application services
  adapter
    in.rest                      -- REST controller
    in.kafka                     -- Kafka consumer
    in.settlement                -- Settlement message handler + local simulator
    out.kafka                    -- Kafka producer
    out.persistence              -- Spring Data JDBC adapter
    out.settlement               -- Logging/mock settlement publisher
  config                         -- Spring @Configuration classes
```

## Domain Model

- `BetStatus` — enum: `PENDING`, `WON`, `LOST`
- `Bet` — record: `id`, `userId`, `eventId`, `eventMarketId`, `selectedWinnerId`, `betAmount`, `status`
- `EventOutcome` — record: `eventId`, `eventName`, `eventWinnerId`
- `BetSettlement` — settlement command value object

## Application Ports

**Inbound:**
- `PublishEventOutcomeUseCase` — called by REST adapter
- `HandleEventOutcomeUseCase` — called by Kafka consumer; matches bets + publishes settlement commands
- `SettleBetUseCase` — called by settlement handler; settles a single bet

**Outbound:**
- `EventOutcomePublisher` → `KafkaEventOutcomePublisher`
- `BetRepository` → `JdbcBetRepositoryAdapter`
- `BetSettlementPublisher` → `LoggingBetSettlementPublisher`

## Settlement Flow (RocketMQ Mock)

Publishing and consuming are always separate concerns:
- `LoggingBetSettlementPublisher` — logs settlement command as JSON (mock publisher)
- `BetSettlementMessageHandler` — handles settlement commands
- `LocalBetSettlementSimulator` — active on `local`/`test` profile; bridges publisher → handler

## Key Decisions

- `out/` in `.gitignore` changed to `/out/` (root-only) to avoid ignoring `application/port/out/`
- V1 migration in `src/main/resources/db/migration/`
- V2 seed migration in `src/test/resources/db/migration/`
- Correlation ID via `X-Correlation-ID` header, propagated through Kafka headers and MDC
