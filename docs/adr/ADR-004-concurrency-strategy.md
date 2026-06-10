# ADR-004: Concurrency control strategy

## Status
Accepted

## Decision
Three mechanisms, matched to contention shape:

1. Optimistic locking (issues.version + JPA @Version) for issue updates.
   Concurrent PATCH/transition with a stale version -> 409 VERSION_CONFLICT
   including currentVersion + current state so clients can merge-and-retry
   (Scenario 1). Chosen because issue edit conflicts are rare; locking reads
   would punish the common case.

2. Postgres transaction-scoped advisory locks (acquire_tx_lock) for sprint
   start/complete (serialized per project: enforces the single-active-sprint
   invariant) and for WIP-limit checks (serialized per target status so
   count-then-move can't race two issues past the limit). Chosen over row
   locks because the invariants span MULTIPLE rows ("no other active sprint",
   "count of issues in column"), which row-level locking can't express safely.

3. Idempotency keys for POST retries: same key + same body replays the stored
   response; same key + different body -> 422.

## Why not SELECT ... FOR UPDATE everywhere
Pessimistic locking on the issues table would serialize the board's write path
and create lock-wait queues under the 500-concurrent-user target. Advisory
locks scope the serialization to exactly the invariant being protected.
