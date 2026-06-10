# ADR-005: WebSocket reconnection + missed-event replay

## Status
Accepted

## Decision
Every domain event gets a per-project monotonic sequence (project_seq, allocated
by next_event_seq() in the same transaction as the mutation). Clients track the
highest seq they've seen; on reconnect they pass ?lastSeq=N and the server
replays domain_events WHERE project_seq > N in order before live traffic resumes.

Delivery is at-least-once (the outbox poller marks dispatched_at after a
successful broadcast); clients de-dupe by seq. Presence join/leave events are
ephemeral and intentionally NOT replayed.

## Alternatives rejected
- Redis Streams per project: another moving part; the outbox table already
  exists and is transactional with the source mutation.
- Server-side per-client cursors: stateful, complicates horizontal scaling.
  Client-held cursors keep the server stateless (see ADR-006).
