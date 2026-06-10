# ADR-003: CQRS-lite for the board read model (no event sourcing)

## Status
Accepted

## Context
The board endpoint is the hottest read path (target: 100 concurrent viewers).
The PDF's bar-raiser asks for a CQRS read model for board views.

## Decision
Read side: a dedicated board query (issues by project + batch user fetch -> zero
N+1) serialized once and cached in Redis (30s TTL). Write side: normal JPA
aggregates. Cache invalidation is event-driven: the outbox dispatcher evicts the
project's board key on every domain event, so staleness is bounded by dispatch
latency (~200ms), not the TTL.

We did NOT adopt full event sourcing (rebuilding state from domain_events):
for a 12-15h take-home it doubles complexity (projections, rebuilds, snapshots)
without changing externally observable behavior. The domain_events table already
gives us the audit/replay benefits.

## Consequences
+ Board reads are O(1) Redis hits when warm; DB sees one query per eviction.
+ Write model stays simple and transactional.
- Two sources of truth for ~200ms windows (acceptable: WS events reconcile clients).
