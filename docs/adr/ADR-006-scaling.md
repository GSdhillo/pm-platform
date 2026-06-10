# ADR-006: Scaling strategy & state placement

## Status
Accepted

## What is stateless / stateful
- App instances: stateless EXCEPT live WebSocket sessions (in-memory registry).
  JWTs carry identity; rate limits, presence, and board cache live in Redis;
  replay cursors are client-held.
- Postgres: source of truth. Redis: disposable cache/coordination (loss = cold
  cache + presence blip, no data loss).

## Scaling to ~500 concurrent users
A single instance handles this comfortably (board reads are Redis hits; writes
are short transactions). Horizontal scale-out path, already compatible with the
design:
1. N app instances behind a load balancer (sticky sessions NOT required for
   REST; WS connections pin naturally to their instance).
2. WS fan-out across instances: today each instance polls the outbox and
   broadcasts to ITS sessions - this already works multi-instance because every
   instance sees every event. (Trade-off: duplicate notification fanout would
   need the dispatcher to use SELECT ... FOR UPDATE SKIP LOCKED, a one-line
   change documented here for honesty.)
3. Postgres: read replicas for board/search reads; PgBouncer for connection
   pooling beyond ~10 instances.
4. Sharding (only if far beyond current targets): shard by project_id - every
   table already carries it and no query joins across projects.

## Bottlenecks, in expected order
board query (mitigated: cache) -> FTS on comments (mitigated: GIN) ->
outbox polling frequency -> WS fan-out CPU.
