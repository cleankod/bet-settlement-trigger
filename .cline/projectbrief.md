# Project Brief — Bet Settlement Trigger

## What This Is

A backend service that simulates sports betting event outcome handling and bet settlement.

## Required Flow

1. REST API receives a sports event outcome (`POST /api/v1/event-outcomes`).
2. REST adapter publishes the outcome to Kafka topic `event-outcomes`.
3. Kafka consumer listens to `event-outcomes`.
4. Application matches the outcome to pending bets stored in an in-memory database.
5. Settlement commands are published to the settlement messaging port (RocketMQ, mocked).
6. Settlement message handler consumes settlement commands and settles matched bets.

## Key Constraints

- Assignment scope: ~90 minutes of work, production-shaped slice
- RocketMQ is mocked via a logging adapter (assignment explicitly allows this)
- H2 in-memory database for bets
- Delivery guarantee: at-least-once; consumers must be idempotent
- Already-settled bets must not be re-settled

## Repository

- GitHub: `git@spyro.github.com:cleankod/bet-settlement-trigger.git`
- Branch strategy: feature branches, reviewed + merged to `master` via PR
