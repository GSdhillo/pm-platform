# ADR-001: Stack — Spring Boot + PostgreSQL (single store) + Redis

**Status:** Accepted

## Context
The assignment needs a relational model, full-text search, an event-driven core,
real-time sync, and a hosted demo — built in ~12-15 hours.

## Decision
- **Java 21 / Spring Boot 3.3** — mature ecosystem for every required concern
  (JPA, WebSocket, Actuator probes, Micrometer, Resilience4j).
- **PostgreSQL as the single source of truth**, including:
  - Full-text search via generated `tsvector` columns + GIN indexes
    (instead of Elasticsearch)
  - Event store / transactional outbox (`domain_events`) instead of Kafka
  - Advisory locks for sprint lifecycle instead of a distributed lock service
- **Redis** only for ephemeral state: board-view cache, rate-limit counters, presence.

## Consequences
- (+) One transactional boundary: domain mutation + audit event + outbox row commit
  atomically. No dual-write problem, no eventual-consistency bugs in a 2-day build.
- (+) docker-compose with 3 containers; trivially hostable for the demo.
- (-) Postgres FTS is weaker than Elasticsearch for relevance ranking — acceptable
  at this scale (~500 users/workspace).
- (-) Outbox polling adds small dispatch latency (~100ms) vs a broker push.
  At larger scale we would promote the outbox to Debezium CDC -> Kafka without
  changing domain code (the dispatcher is an adapter behind a port).
