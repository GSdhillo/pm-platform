# ADR-002: Transactional outbox as the single event backbone

**Status:** Accepted

## Context
The PDF requires: full audit trail, activity feed, domain events for all mutations,
WebSocket broadcast with missed-event replay, and notifications that survive a
downstream outage (Scenario 4).

## Decision
Every mutation writes a `domain_events` row in the same DB transaction as the change,
with a per-project monotonic `project_seq`. A poller dispatches undispatched events to:
1. WebSocket broadcaster (clients replay from their last seen `project_seq` on reconnect)
2. Notification service (behind a Resilience4j circuit breaker; failures leave
   notifications in PENDING for later retry)
3. Activity feed reads the table directly (no projection needed; cursor = `id`)

## Alternatives considered
- Spring ApplicationEvents only: lost on crash, no replay, no audit. Rejected.
- Kafka: operationally heavy for the demo, dual-write problem unless paired with
  an outbox anyway. Rejected for this scope, noted as the scale-up path.
