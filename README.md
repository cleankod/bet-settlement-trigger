# Bet Settlement Trigger

A backend service that handles sports event outcome notifications and triggers bet settlement.
Built as a home assignment to demonstrate production-shaped Spring Boot architecture, Kafka
integration, and clean hexagonal design — scoped to roughly 90 minutes of implementation time.

---

## Table of Contents

- [Assignment Summary](#assignment-summary)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Running Tests](#running-tests)
- [API Reference](#api-reference)
- [Architecture Overview](#architecture-overview)
- [Observability](#observability)
- [Reliability Assumptions](#reliability-assumptions)
- [Trade-offs and Design Decisions](#trade-offs-and-design-decisions)
- [Given More Time](#given-more-time)
- [AI Usage Disclosure](#ai-usage-disclosure)

---

## Assignment Summary

The service simulates the outcome-handling and bet-settlement pipeline for a sports betting platform:

1. A REST endpoint accepts a sports event outcome (`POST /api/v1/event-outcomes`).
2. The REST adapter publishes the outcome to the Kafka topic `event-outcomes`.
3. A Kafka consumer picks up the message and matches it against pending bets stored in an in-memory H2 database.
4. For each matched pending bet, a settlement command is published to the settlement messaging port.
5. The settlement publisher delivers the command and settles the bet (WON or LOST).
6. RocketMQ is represented by a mock/logging adapter. Under the `local` profile,
   `LocalBetSettlementPublisher` calls `SettleBetUseCase` directly, enabling full end-to-end
   flow without a real RocketMQ broker.

---

## Prerequisites

| Tool                    | Version                |
|-------------------------|------------------------|
| Java                    | 25                     |
| Docker + Docker Compose | Any recent version     |
| Gradle                  | Wrapper included (`./gradlew`) |

Verify your JDK before starting:

```bash
java -version   # should report 25
```

---

## Quick Start

### 1. Start infrastructure (Kafka)

```bash
docker compose up -d kafka
```

Wait for Kafka to be healthy:

```bash
docker compose ps
```

### 2. Run the application

```bash
./gradlew bootRun
```

The application starts on port `8080`. Kafka bootstrap address defaults to `localhost:9092`.

To activate `LocalBetSettlementPublisher` for full end-to-end settlement without RocketMQ:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 3. Place a bet

```bash
curl -s -X POST http://localhost:8080/api/v1/bets \
  -H 'Content-Type: application/json' \
  -d '{"userId":"user-alice","eventId":"match-101","eventMarketId":"market-main","selectedWinnerId":"team-alpha","betAmount":50.00}' \
  -w '\nHTTP %{http_code}\n'
```

Expected response: `HTTP 201` with a `Location: /api/v1/bets/1` header.

### 4. Submit an event outcome

```bash
curl -s -X POST http://localhost:8080/api/v1/event-outcomes \
  -H 'Content-Type: application/json' \
  -d '{"eventId":"match-101","eventName":"League Final","eventWinnerId":"team-alpha"}' \
  -w '\nHTTP %{http_code}\n'
```

Expected response: `HTTP 202`

With the `local` profile active, the bet placed in step 3 will be settled asynchronously —
check the application logs to see the settlement command and the status update.

---

## Docker Compose

The `docker-compose.yml` includes:

| Service            | Port    | Description                                                 |
|--------------------|---------|-------------------------------------------------------------|
| `kafka`            | `9092`  | Apache Kafka 4.0 in KRaft mode (no ZooKeeper)               |
| `rocketmq-namesrv` | `9876`  | RocketMQ name server (optional — service is mocked in code) |
| `rocketmq-broker`  | `10911` | RocketMQ broker (optional)                                  |

Start all services:

```bash
docker compose up -d
```

Stop and remove:

```bash
docker compose down
```

---

## Running Tests

Requires Docker (Testcontainers spins up a real Kafka container):

```bash
./gradlew test
```

The test suite includes:

| Test                                                        | Type              | What it verifies                                  |
|-------------------------------------------------------------|-------------------|---------------------------------------------------|
| `returns201CreatedWithLocationHeader`                       | Integration       | Bet placement returns 201 + Location header       |
| `returns400WhenBetAmountIsZero`                             | Integration       | Validation rejects zero bet amount                |
| `returns202AcceptedForValidRequest`                         | Integration       | Event outcome endpoint accepts valid payload      |
| `returns400WhenEventIdIsMissing`                            | Integration       | Bean Validation rejects incomplete request        |
| `returns400WhenEventWinnerIdIsMissing`                      | Integration       | Bean Validation rejects incomplete request        |
| `settlesBetAsWonWhenSelectedWinnerMatchesActualWinner`      | Integration (E2E) | Full REST→Kafka→settle WON flow                   |
| `settlesBetAsLostWhenSelectedWinnerDiffersFromActualWinner` | Integration (E2E) | Full REST→Kafka→settle LOST flow                  |
| `doesNotResettleAlreadySettledBet`                          | Integration (E2E) | Idempotency — duplicate outcome is a no-op        |
| `doesNotFailWhenNoMatchingBetsExistForEvent`                | Integration       | Graceful handling of events with no bets          |
| `BetTest` (2 tests)                                         | Unit              | Domain logic: `Bet.settle()` WON/LOST/idempotency |

---

## Running MkDocs

Internal documentation is in `docs/` using [MkDocs](https://www.mkdocs.org/) with the Material theme.

```bash
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
mkdocs serve
```

Open `http://127.0.0.1:8000` in your browser.

---

## API Reference

### POST /api/v1/bets

Places a new bet. Returns `201 Created` with a `Location` header pointing to the created bet resource.

**Request:**

```json
{
  "userId": "user-alice",
  "eventId": "match-101",
  "eventMarketId": "market-main",
  "selectedWinnerId": "team-alpha",
  "betAmount": 50.00
}
```

| Field              | Type    | Required | Description                                |
|--------------------|---------|----------|--------------------------------------------|
| `userId`           | string  | ✅        | Identifier of the user placing the bet     |
| `eventId`          | string  | ✅        | Identifier of the event the bet is for     |
| `eventMarketId`    | string  | ✅        | Identifier of the market within the event  |
| `selectedWinnerId` | string  | ✅        | The participant the user predicts will win |
| `betAmount`        | decimal | ✅        | Stake amount (must be > 0)                 |

**Responses:**

| Status            | Description                                  |
|-------------------|----------------------------------------------|
| `201 Created`     | Bet placed; `Location: /api/v1/bets/{betId}` |
| `400 Bad Request` | Validation failure — see error body          |

---

### POST /api/v1/event-outcomes

Accepts a sports event outcome for processing. The outcome is published asynchronously to Kafka;
the response is returned immediately after successful publication.

**Request:**

```json
{
  "eventId": "match-101",
  "eventName": "League Final",
  "eventWinnerId": "team-alpha"
}
```

| Field           | Type   | Required | Description                                   |
|-----------------|--------|----------|-----------------------------------------------|
| `eventId`       | string | ✅        | Unique identifier of the event                |
| `eventName`     | string | ✅        | Human-readable event name (carried for audit) |
| `eventWinnerId` | string | ✅        | Identifier of the winning team/participant    |

**Responses:**

| Status                    | Description                         |
|---------------------------|-------------------------------------|
| `202 Accepted`            | Outcome accepted for publication    |
| `400 Bad Request`         | Validation failure — see error body |
| `503 Service Unavailable` | Publication to Kafka failed         |

**Error response body:**

```json
{
  "errorId": "550e8400-e29b-41d4-a716-446655440000",
  "code": "VALIDATION_FAILURE",
  "message": "Request validation failed",
  "details": [
    "eventId: must not be blank"
  ]
}
```

**Correlation ID:**

Pass `X-Correlation-ID: <uuid>` in the request header. If absent, the service generates one.
The correlation ID is propagated through Kafka headers and appears in all log entries for the
request lifecycle.

---

## Architecture Overview

The service uses a lightweight **Hexagonal Architecture** (Ports and Adapters):

```
┌──────────────────────────────────────┐
│          Inbound Adapters            │
│  REST Controller  │  Kafka Consumer  │
└────────┬──────────┴────────┬─────────┘
         │                   │
         ▼                   ▼
┌──────────────────────────────────────┐
│     Application Layer (Use Cases)    │
│  PublishEventOutcomeUseCase          │
│  HandleEventOutcomeUseCase           │
│  SettleBetUseCase ◄────────────────────────┐
└──────────────────────────────────────┘     │
         │                              Outbound Adapters
         ▼                          ┌──────────────────────┐
┌──────────────────┐                │  Kafka publisher     │
│   Domain Model   │                │  JDBC repository     │
│  Bet lifecycle   │                │  Settlement publisher│
└──────────────────┘                └──────────────────────┘
```

**Package layout:**

```
eu.cleankod.settlementtrigger
  domain/               ← Pure domain — no framework dependencies
  application/
    port/in/            ← Inbound use case interfaces
    port/out/           ← Outbound port interfaces (repository, publishers)
    service/            ← Use case implementations
  adapter/
    in/rest/            ← REST controller, request/response DTOs, error handling
    in/kafka/           ← Kafka consumer
    out/kafka/          ← Kafka producer (EventOutcomePublisher impl)
    out/persistence/    ← Spring Data JDBC adapter (BetRepository impl)
    out/settlement/     ← Settlement publisher (two profile-exclusive implementations)
  config/               ← Spring configuration (Kafka, correlation ID filter)
```

**Settlement flow (local profile):**

```
REST POST → Kafka (event-outcomes) → Kafka Consumer
  → find pending bets → publish BetSettlement command
    → LocalBetSettlementPublisher → SettleBetUseCase → save settled Bet
```

Under the `!local` profile, `LocalBetSettlementPublisher` is replaced by `LoggingBetSettlementPublisher`
which logs the command as JSON; a real RocketMQ consumer would deliver to `SettleBetUseCase` instead.

---

## Observability

### Health and metrics

Spring Boot Actuator is enabled with the following endpoints:

| Endpoint | URL                     |
|----------|-------------------------|
| Health   | `GET /actuator/health`  |
| Info     | `GET /actuator/info`    |
| Metrics  | `GET /actuator/metrics` |

### Structured logging

Logs are in structured format via Logback. Each log line includes:

- Timestamp
- Thread name
- Log level
- **Correlation ID** (from MDC, propagated from `X-Correlation-ID` header or Kafka header)
- Logger name
- Message

Example:

```
2026-06-25 10:30:00.123 [http-nio-8080-exec-1] INFO  [abc123] e.c.s.a.i.rest.EventOutcomeController - Received event outcome [eventId=match-101]
```

### Correlation ID propagation

- Inbound: read from `X-Correlation-ID` header (generated if absent), stored in MDC
- Outbound Kafka: written as a Kafka message header `correlationId` (note: different name from the HTTP header)
- Inbound Kafka: read from Kafka header, restored to MDC for the consumer thread

---

## Reliability Assumptions

### Delivery guarantee: at-least-once

Kafka is configured with `auto-offset-reset=earliest` and default acknowledgement. Messages
may be redelivered on consumer restart. The service handles this safely through idempotency.

### Idempotent settlement

Settlement is idempotent at two layers:

1. **`EventOutcomeHandlingService`** — `findPendingByEventId` only returns bets in `PENDING` status.
   If a bet is already settled when a duplicate event outcome arrives, it is not found and no
   settlement command is published.

2. **`BetSettlementService`** — If a settlement command arrives for an already-settled bet, the
   service logs and skips it.

### RocketMQ is mocked

`LoggingBetSettlementPublisher` logs settlement commands as JSON instead of publishing to a real
RocketMQ broker. Under the `local` profile, `LocalBetSettlementPublisher` calls `SettleBetUseCase`
directly, enabling full end-to-end flow in development and test environments without a real broker.

This is an intentional trade-off for the assignment scope — see [Given More Time](#given-more-time).

---

## Trade-offs and Design Decisions

| Decision                               | Rationale                                                                                                                                             |
|----------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| Hexagonal Architecture                 | Proposed by the author to cleanly separate domain, application, and adapter concerns; allows swapping Kafka or RocketMQ without touching domain logic |
| Spring Data JDBC (not JPA)             | Explicit SQL, no lazy-loading surprises, simpler mapping; sufficient for the assignment scope                                                         |
| H2 in-memory database                  | No external dependency; Flyway migrations keep schema management realistic                                                                            |
| `UnsavedBet` + `Bet` two-type model    | Type safety: impossible to accidentally persist a `Bet` without a database id, or treat an `UnsavedBet` as if it has one                              |
| RocketMQ mocked                        | Assignment explicitly allows it; real RocketMQ setup would add significant infra complexity for no functional gain at this scope                      |
| At-least-once delivery                 | Exactly-once requires transactional outbox + idempotency keys; the simpler approach is correct idempotency checks, which are implemented              |
| Kafka publish is synchronous (bounded) | `.get(timeout)` on the Kafka `Future` makes publish failures visible at the REST layer with a clean 500 response                                      |
| `spring-boot-starter-kafka`            | The bare `spring-kafka` library lacks `KafkaAutoConfiguration`, which means `@KafkaListener` is never activated — identified and fixed during review  |

---

## Given More Time

The following improvements are out of scope for this assignment but would be prioritised next
in a production implementation:

- **Value objects** — Wrap primitive identifiers (`eventId`, `userId`, `betId`) in typed value
  objects to eliminate stringly-typed parameters and strengthen domain invariants
- **Observability and metrics** — Business metrics (bets settled, WON/LOST counts, event outcomes
  received), technical metrics (Kafka consumer lag, publish latency), and distributed tracing
  via OpenTelemetry with Prometheus + Grafana dashboards
- **ArchUnit tests** — Enforce hexagonal architecture constraints at build time: domain must not
  depend on Spring, adapters must not reach directly into other adapters, etc.
- **Performance and load tests** — Baseline throughput tests, load tests to observe behaviour
  under sustained traffic, and endurance tests to catch long-running degradation (e.g. memory
  leaks, connection pool exhaustion)
- **Penetration tests** — Security testing for the REST surface
- **Real RocketMQ integration** — Complete the `BetSettlementPublisher` implementation against
  a real RocketMQ broker; add a real consumer for the `bet-settlements` topic; add DLQ handling
- **Outbox pattern** — Atomically persist and publish using a transactional outbox table, closing
  the current gap between reading pending bets and publishing their settlement commands
- **Overall cleanup** — Refine package structure, remove any remaining rough edges accumulated
  during iterative development
- **CI/CD pipeline** — Build, test, Docker image publish, and deployment stages (e.g. GitHub Actions
  or GitLab CI)
- **12-Factor App validation** — Review the service against the [12-factor methodology](https://www.12factor.net/)
  to identify configuration, logging, and disposability improvements
- **Concurrency safety** — Add optimistic locking (`@Version` column on `Bet`) to prevent
  TOCTOU races under concurrent settlement commands; transactional outbox to close the
  partial-publish gap in `EventOutcomeHandlingService`; deduplication keys on settlement
  commands to prevent N² publish under concurrent consumers
- **Auto-generated API docs** — Add SpringDoc/OpenAPI dependency to expose a Swagger UI and
  machine-readable OpenAPI spec at `/swagger-ui.html` and `/v3/api-docs`
- **Kubernetes / Helm** — Production deployment manifests
- **BetSettlement command should carry WON/LOST** — currently the downstream consumer re-derives the outcome; the assignment diagram implies
  the command should include it
- **`!local` profile leaves bets permanently PENDING** — the default runtime never settles bets because the RocketMQ publisher only logs; a
  reviewer should be aware this is the intentional mock trade-off
- **Kafka producer configuration via YAML** — remove the custom `KafkaConfig` class; drive the producer entirely through
  `spring.kafka.producer.*` properties and let `KafkaAutoConfiguration` wire the `KafkaTemplate`; add missing properties (`acks`,
  `enable.idempotence`, `retries`, `compression.type`, `client.id`)
- **Kafka consumer DLQ** — add a `DeadLetterPublishingRecoverer` with a fixed retry limit to stop poison messages from blocking the
  partition indefinitely
- **`@ConfigurationProperties` for `app.kafka.*`** — replace `@Value` injection in `KafkaEventOutcomePublisher` with a typed properties
  class for discoverability and validation
- **Parameterize repetitive tests** — `BetTest` has duplicated test pairs differing only by terminal status; consolidate with
  `@ParameterizedTest` + `@EnumSource`; similarly the three `400` event-outcome validation tests should be a single `@MethodSource` test
- **Additional tests** — `EventOutcomeHandlingService` with multiple matching bets, `GlobalExceptionHandler` all three paths,
  `BetSettlement.of()` mismatch throws `EventMismatchException`, `CorrelationIdFilter` header-present/absent/echoed
- **Extract duplicate log format string** — `LocalBetSettlementPublisher` and `LoggingBetSettlementPublisher` share an identical
  `log.info(...)` format; extract to a shared constant or utility

---

## AI Usage Disclosure

This project was developed with significant guidance, direction, and review by the author,
assisted by [Cline](https://github.com/cline/cline), an AI coding assistant powered by Claude.

**Author's contributions:**

- **Technology and architecture decisions** — Java 25, Spring Boot 4, Gradle, Hexagonal Architecture,
  two-type `Bet`/`UnsavedBet` model, and the overall package layout were proposed and directed
  by the author
- **Scope and trade-off decisions** — RocketMQ mock approach, at-least-once delivery choice,
  and feature boundaries were decided by the author
- **Iterative direction** — The author provided ongoing remarks, redirected the implementation
  multiple times, and shaped the final design through review rounds
- **Code review** — The author identified issues (deprecated `JsonDeserializer`, dead YAML producer
  configuration, weak idempotency test assertion, Spring Boot starter vs. bare library) and
  directed their fixes

**AI contributions:**

- Scaffolding, boilerplate, and implementation of approved designs
- Writing tests, documentation, and commit messages following author-defined rules
- Identifying some code quality issues during review

All generated code was reviewed and approved by the author before committing.
