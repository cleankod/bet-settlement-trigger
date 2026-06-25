# Technical Decisions

This page documents key technical decisions made during development, their rationale,
and the trade-offs involved.

## Architecture: Hexagonal (Ports and Adapters)

**Decision:** Structure the codebase around Hexagonal Architecture with a pure domain,
application use cases, and framework-specific adapters.

**Rationale:** Proposed by the author to achieve a clean separation of concerns. The domain
and application layers contain no Spring, Kafka, JDBC, or any other infrastructure code.
This means Kafka could be replaced with a different broker, or H2 with a real database,
without touching domain logic. Adapters are the only place where technology choices leak.

**Trade-off:** More files and indirection than a simple layered architecture. Acceptable
because the design maps directly to the assignment's required flow and is easy to explain.

---

## Domain model: `Bet` + `UnsavedBet` two-type model

**Decision:** Use two distinct types — `UnsavedBet` (no id, before persistence) and
`Bet` (primitive `long id`, guaranteed by type).

**Rationale:** Suggested by the author to enforce a compiler-level guarantee. It is impossible
to accidentally use an unsaved bet where a persisted bet is expected, and vice versa.
The persistence adapter maps between them — the domain never sees a nullable id.

**Trade-off:** Slightly more ceremony when creating bets. Eliminated an entire class of
runtime null-id bugs.

---

## Persistence: Spring Data JDBC (not JPA/Hibernate)

**Decision:** Use Spring Data JDBC with explicit SQL migrations.

**Rationale:** Spring Data JDBC avoids JPA's lazy-loading pitfalls, session management,
and implicit query generation. SQL is explicit and predictable. Flyway migrations make the
schema evolution visible and reviewable.

**Trade-off:** More manual mapping code (`BetEntity` ↔ `Bet`). Acceptable for the assignment
scope; production-scale would benefit from the same approach.

---

## Kafka: synchronous bounded publish

**Decision:** `KafkaTemplate.send(...).get(timeout)` — block the REST thread for up to 5 seconds.

**Rationale:** Makes Kafka unavailability immediately visible to the caller as a `500` response
rather than silently accepting the request and losing the event. The timeout is configurable
(`app.kafka.publish-timeout`).

**Trade-off:** REST latency increases if Kafka is slow. Acceptable for assignment scope;
production would use async publish with a circuit breaker.

---

## RocketMQ: mocked by `LoggingBetSettlementPublisher`

**Decision:** Replace real RocketMQ publish with a logging adapter that writes settlement
commands as JSON.

**Rationale:** The assignment explicitly allows mocking RocketMQ. A real RocketMQ setup
requires running a name server + broker, configuring the Java client, handling reconnects,
DLQ, and retries — significant operational complexity for a home assignment with no additional
functional value.

The `LocalBetSettlementSimulator` (`@Profile("local")`) connects the publisher directly to
the settlement handler in-process, enabling a complete end-to-end flow in development and
integration tests without a real broker.

**Trade-off:** Settlement commands are not durable. Documented as a future improvement.

---

## Settlement messaging: publisher and consumer are separate components

**Decision:** Keep `BetSettlementPublisher` (out port) and `BetSettlementMessageHandler`
(in adapter) as separate components. The `LocalBetSettlementSimulator` wires them together
only under the `local` profile.

**Rationale:** If the consumer logic were hidden inside the publisher, swapping in a real
RocketMQ implementation would require untangling them. Keeping them separate means the
real RocketMQ publisher can be added without touching the handler.

---

## Testing: integration tests over unit mocking

**Decision:** Prefer integration tests that exercise the full stack through real entry points.
Unit tests only for pure domain logic (`BetTest`).

**Rationale:** Per project rules, integration tests are the primary safety net. The end-to-end
tests (`EventOutcomeIntegrationTest`) use Testcontainers Kafka and H2 to verify the complete
REST → Kafka → settle flow. This tolerates internal refactoring without test breakage.

**Trade-off:** Tests are slower (Testcontainers startup). Acceptable — the suite runs in ~15s.

---

## Kafka starter: `spring-boot-starter-kafka` not `spring-kafka`

**Decision:** Use `org.springframework.boot:spring-boot-starter-kafka` as the Kafka dependency.

**Rationale:** The bare `spring-kafka` library does not include `KafkaAutoConfiguration`.
Without the starter, `@KafkaListener` consumer infrastructure is never activated — messages
are published but never consumed. Identified and fixed during review.

---

## Consumer deserializer: `JacksonJsonDeserializer` (Jackson 3)

**Decision:** Use `org.springframework.kafka.support.serializer.JacksonJsonDeserializer`
with `spring.json.java-type-info.default-type`.

**Rationale:** The producer uses `JacksonJsonSerializer` (Jackson 3). Using the deprecated
`JsonDeserializer` (Jackson 2) created an asymmetry that would become a breaking migration
cliff when the old class is eventually removed. Identified and corrected during review.
