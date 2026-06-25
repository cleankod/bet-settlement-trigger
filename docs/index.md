# Bet Settlement Trigger — Overview

Bet Settlement Trigger is a backend service that handles sports event outcome notifications
and drives bet settlement. It is built as a home assignment demonstrating a production-shaped
slice of a betting platform using Hexagonal Architecture, Spring Boot 4, Kafka, and an
H2 in-memory database.

## What the service does

1. Exposes `POST /api/v1/event-outcomes` — accepts a sports event outcome
2. Publishes the outcome to the Kafka topic `event-outcomes`
3. Consumes the outcome from Kafka, queries pending bets for that event
4. Publishes a settlement command for each pending bet
5. Settles each bet as WON or LOST based on whether the selected winner matches the actual winner

## Technology stack

| Layer                | Technology                                                |
|----------------------|-----------------------------------------------------------|
| Runtime              | Java 25                                                   |
| Framework            | Spring Boot 4                                             |
| Messaging (inbound)  | Apache Kafka (KRaft, no ZooKeeper)                        |
| Messaging (outbound) | RocketMQ — mocked by `LoggingBetSettlementPublisher`      |
| Persistence          | Spring Data JDBC + H2 in-memory + Flyway                  |
| Build                | Gradle (Groovy DSL) with version catalog                  |
| Tests                | JUnit 5 + AssertJ + Testcontainers + Awaitility           |
| Observability        | Spring Boot Actuator + SLF4J/Logback + MDC correlation ID |

## Navigation

- [Architecture](architecture.md) — hexagonal design, package layout, component responsibilities
- [API](api.md) — REST endpoint contract, request/response formats, error handling
- [Reliability](reliability.md) — delivery guarantees, idempotency, RocketMQ trade-off
- [Decisions](decisions.md) — key technical decisions and their rationale
