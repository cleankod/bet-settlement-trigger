# API Reference

## POST /api/v1/bets

Places a new bet in the system. The bet is stored in `PENDING` status and will be matched
against event outcomes when they arrive.

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

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `userId` | string | ✅ | not blank |
| `eventId` | string | ✅ | not blank |
| `eventMarketId` | string | ✅ | not blank |
| `selectedWinnerId` | string | ✅ | not blank — the participant the user predicts will win |
| `betAmount` | decimal | ✅ | > 0 |

**Responses:**

| Status | Description |
|--------|-------------|
| `201 Created` | Bet placed; `Location` header contains `/api/v1/bets/{betId}` |
| `400 Bad Request` | Validation failure — see error body |

---

## POST /api/v1/event-outcomes

Accepts a sports event outcome for processing. The outcome is accepted synchronously and
published to Kafka. The response is returned as soon as the message is confirmed published;
actual bet settlement happens asynchronously.

### Request

**Content-Type:** `application/json`

```json
{
  "eventId": "match-101",
  "eventName": "League Final",
  "eventWinnerId": "team-alpha"
}
```

| Field           | Type   | Required | Constraints | Description                                           |
|-----------------|--------|----------|-------------|-------------------------------------------------------|
| `eventId`       | string | ✅        | not blank   | Unique identifier of the sports event                 |
| `eventName`     | string | ✅        | not blank   | Human-readable event name; carried for audit purposes |
| `eventWinnerId` | string | ✅        | not blank   | Identifier of the winning team or participant         |

### Responses

#### 202 Accepted

The outcome was successfully published to Kafka for processing.

```
HTTP/1.1 202 Accepted
```

No response body.

#### 400 Bad Request

One or more request fields failed validation.

```json
{
  "errorId": "550e8400-e29b-41d4-a716-446655440000",
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "details": [
    "eventId: must not be blank"
  ]
}
```

| Field     | Description                                                              |
|-----------|--------------------------------------------------------------------------|
| `errorId` | Unique identifier for this error occurrence — useful for log correlation |
| `code`    | Machine-readable error category                                          |
| `message` | Human-readable summary                                                   |
| `details` | List of individual validation violations (field: message format)         |

#### 500 Internal Server Error

Publication to Kafka failed. The Kafka publish is bounded by `app.kafka.publish-timeout`
(default 5 seconds); a timeout or broker unavailability results in a 500 response.

```json
{
  "errorId": "a1b2c3d4-...",
  "code": "INTERNAL_ERROR",
  "message": "Failed to publish event outcome"
}
```

### Correlation ID

The service supports request tracing via the `X-Correlation-ID` header.

- If present in the request, the value is used as-is and propagated through the entire flow
- If absent, a new UUID is generated
- The correlation ID is stored in MDC (`correlationId` key) and appears in all log entries
- It is propagated to Kafka as a message header and restored by the consumer

```bash
curl -X POST http://localhost:8080/api/v1/event-outcomes \
  -H 'Content-Type: application/json' \
  -H 'X-Correlation-ID: my-trace-id-123' \
  -d '{"eventId":"match-101","eventName":"League Final","eventWinnerId":"team-alpha"}'
```

## Actuator endpoints

Spring Boot Actuator is exposed on the same port (`8080`):

| Endpoint                   | Method | Description                                       |
|----------------------------|--------|---------------------------------------------------|
| `/actuator/health`         | GET    | Application health including datasource and Kafka |
| `/actuator/info`           | GET    | Build and application metadata                    |
| `/actuator/metrics`        | GET    | Available Micrometer metrics                      |
| `/actuator/metrics/{name}` | GET    | Specific metric by name                           |
