# Architecture

The service is structured around a lightweight **Hexagonal Architecture** (Ports and Adapters).
The core principle is that the domain and application layers have no knowledge of Spring, Kafka,
JDBC, or any other infrastructure technology. All framework dependencies are contained in adapters.

## Layers

### Domain

Pure Java — no framework annotations, no Spring, no persistence. Contains:

- `Bet` — a persisted bet with a guaranteed database id (primitive `long`)
- `UnsavedBet` — a bet before persistence (no id field)
- `BetStatus` — `PENDING`, `WON`, `LOST`
- `EventOutcome` — the sports event outcome received from the REST API
- `BetSettlement` — the settlement command carrying bet id, event winner, and correlation id
- `BetAlreadySettledException` — thrown by `Bet.settle()` if the bet is not `PENDING`
- `EventMismatchException` — thrown if a settlement command references a different event than the bet

The two-type model (`Bet` vs. `UnsavedBet`) enforces a compiler-level guarantee that a bet
without a database id can never be accidentally used where a persisted bet is expected.

### Application

Orchestrates use cases. Depends on domain and port interfaces only — never on adapters.

**Inbound ports (use case interfaces):**

| Interface                    | Called by                  |
|------------------------------|----------------------------|
| `PublishEventOutcomeUseCase` | REST controller            |
| `HandleEventOutcomeUseCase`  | Kafka consumer             |
| `SettleBetUseCase`           | Settlement message handler |

**Outbound ports:**

| Interface                | Implemented by                  |
|--------------------------|---------------------------------|
| `BetRepository`          | `JdbcBetRepositoryAdapter`      |
| `EventOutcomePublisher`  | `KafkaEventOutcomePublisher`    |
| `BetSettlementPublisher` | `LoggingBetSettlementPublisher` |

**Services:**

| Service                         | Responsibility                                                          |
|---------------------------------|-------------------------------------------------------------------------|
| `EventOutcomePublishingService` | Delegates to `EventOutcomePublisher`                                    |
| `EventOutcomeHandlingService`   | Finds pending bets by event ID; publishes a settlement command for each |
| `BetSettlementService`          | Sole writer of bet state; settles a bet and persists the updated status |

### Adapters

All framework code lives here. Adapters implement or consume ports.

**Inbound:**

| Adapter                       | Technology                    | Purpose                                                                                            |
|-------------------------------|-------------------------------|----------------------------------------------------------------------------------------------------|
| `EventOutcomeController`      | Spring MVC                    | REST endpoint; validates input; calls `PublishEventOutcomeUseCase`                                 |
| `EventOutcomeKafkaConsumer`   | Spring Kafka `@KafkaListener` | Consumes from `event-outcomes`; calls `HandleEventOutcomeUseCase`                                  |
| `BetSettlementMessageHandler` | Plain component               | Consumes settlement commands; calls `SettleBetUseCase`                                             |
| `LocalBetSettlementSimulator` | `@Profile("local")`           | Wires `LoggingBetSettlementPublisher` callback to `BetSettlementMessageHandler` for local/test use |

**Outbound:**

| Adapter                         | Technology             | Purpose                                                      |
|---------------------------------|------------------------|--------------------------------------------------------------|
| `KafkaEventOutcomePublisher`    | Spring `KafkaTemplate` | Publishes `EventOutcome` to `event-outcomes` topic           |
| `JdbcBetRepositoryAdapter`      | Spring Data JDBC       | Implements `BetRepository` — maps `BetEntity` ↔ domain `Bet` |
| `LoggingBetSettlementPublisher` | SLF4J                  | Mock RocketMQ — logs settlement command as JSON              |

## Package layout

```
eu.cleankod.settlementtrigger
  domain/
    Bet.java
    UnsavedBet.java
    BetStatus.java
    EventOutcome.java
    BetSettlement.java
    BetAlreadySettledException.java
    EventMismatchException.java
  application/
    port/
      in/
        PublishEventOutcomeUseCase.java
        HandleEventOutcomeUseCase.java
        SettleBetUseCase.java
      out/
        BetRepository.java
        EventOutcomePublisher.java
        BetSettlementPublisher.java
    service/
      EventOutcomePublishingService.java
      EventOutcomeHandlingService.java
      BetSettlementService.java
  adapter/
    in/
      rest/
        EventOutcomeController.java
        EventOutcomeRequest.java
        GlobalExceptionHandler.java
        ErrorResponse.java
        ErrorIdGenerator.java
      kafka/
        EventOutcomeKafkaConsumer.java
      settlement/
        BetSettlementMessageHandler.java
        LocalBetSettlementSimulator.java
    out/
      kafka/
        KafkaEventOutcomePublisher.java
      persistence/
        JdbcBetRepositoryAdapter.java
        BetEntity.java
        SpringDataBetRepository.java
      settlement/
        LoggingBetSettlementPublisher.java
  config/
    KafkaConfig.java
    KafkaTopics.java
    CorrelationId.java
    CorrelationIdFilter.java
```

## Settlement flow

```
POST /api/v1/event-outcomes
  │
  ▼ EventOutcomeController
  │   validates request, maps to EventOutcome
  ▼ EventOutcomePublishingService
  │   calls EventOutcomePublisher port
  ▼ KafkaEventOutcomePublisher
  │   publishes to Kafka topic 'event-outcomes'
  │   propagates X-Correlation-ID header
  │
  ▼ EventOutcomeKafkaConsumer (async, separate thread)
  │   reads X-Correlation-ID from Kafka header → MDC
  ▼ EventOutcomeHandlingService
  │   finds all PENDING bets for eventId
  │   for each bet: publishes BetSettlement command
  ▼ LoggingBetSettlementPublisher (mock)
  │   [local profile] invokes BetSettlementMessageHandler directly
  │
  ▼ BetSettlementMessageHandler
  ▼ BetSettlementService
      finds bet by id
      calls Bet.settle(actualWinnerId) → WON or LOST
      saves updated Bet
```

## Dependency rule

The dependency arrow always points inward — domain has no outbound dependencies,
application depends only on domain and its own port interfaces, adapters depend on application.

```
adapters → application → domain
              ↑
            ports
```
