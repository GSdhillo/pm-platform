# PM Platform — Jira-like Project Management Backend

SDE-2 take-home: a production-grade project management backend in Java 21 / Spring Boot 3.

**Demo credentials** (all passwords `password123`): `alice@demo.com` (Admin), `bob@demo.com` (Project Lead), `john@demo.com` (Member), `vera@demo.com` (Viewer). Seeded project: **DEMO** (`aaaaaaaa-0000-0000-0000-000000000001`).

## Run it

```bash
docker compose up --build
# API:      http://localhost:8080/api/v1
# Swagger:  http://localhost:8080/swagger-ui.html
# Health:   http://localhost:8080/api/health/liveness | /readiness
# Metrics:  http://localhost:8080/api/metrics  (Prometheus format)
```

Local dev: `docker compose up postgres redis -d` then run `PmPlatformApplication` (env vars in `application.yml` have local defaults).

## Architecture (hexagonal)

```
adapter/in   : REST controllers (+DTOs), WebSocket handler        <- driving
application  : use-case services, outbox dispatcher, schedulers
domain       : entities, WorkflowEngine (pure logic), ports
adapter/out  : persistence (Spring Data), Redis cache, notification client (circuit breaker)  <- driven
common       : error hierarchy, JWT security, correlation IDs, rate limiting, idempotency
```

**The transactional outbox is the backbone** (ADR-002): every mutation writes a `domain_events` row *in the same transaction*, with a per-project monotonic `project_seq`. That one table powers: the audit trail, the activity feed (cursor on `id`), WebSocket broadcast + reconnect replay (cursor on `project_seq`), and durable notification fanout. A 200ms poller dispatches events to WS clients, creates notifications, and evicts the cached board.

## Requirement traceability

| PDF requirement | Where |
|---|---|
| Data model, hierarchy, custom fields, audit | `domain/model/*`, `V1__core_schema.sql`, `IssueService.validateParent/applyCustomFields`, `domain_events` |
| Workflow engine: statuses, transitions, validators, actions | `WorkflowEngine`, `transition_hooks`, `ProjectController /workflow/*` |
| Sprints: carry-over, velocity | `SprintService.completionPreview/complete/velocity` |
| Comments, @mentions, activity feed, notifications, watchers | `CommentService`, `ActivityService`, `NotificationService`, `issue_watchers` |
| Real-time WS, presence, replay | `BoardWebSocketHandler`, `WsHandshakeInterceptor`, `PresenceService`, ADR-005 |
| Search: FTS + structured + cursors | `SearchService` (Postgres tsvector + GIN; ADR-001) |
| Hexagonal, domain events, CQRS board, API versioning, error hierarchy | package layout, `EventRecorder`, `BoardService` (ADR-003), `/api/v1`, `common/error` |
| Optimistic locking, advisory locks, idempotency, WIP races | `Issue.version`, `acquire_tx_lock`, `IdempotencyFilter`, `IssueService.transition` (ADR-004) |
| Logging w/ correlation IDs, probes, metrics, circuit breaker, graceful shutdown | `CorrelationIdFilter`, actuator config, `ws.connections` gauge, `ExternalNotificationClient`, `BoardWebSocketHandler.destroy()` |
| RBAC, row-level security, validation, rate limits, security audit | `AccessService`, `RateLimitFilter`, `security_audit_log` |

## Scenario walkthroughs

**S1 — concurrent edit:** two clients `GET /issues/{id}` (version 3). Client A `PATCH` with `expectedVersion: 3` → 200, version 4. Client B `PATCH` with `expectedVersion: 3` → **409 VERSION_CONFLICT** with `details.currentVersion: 4` + current state to merge.

**S2 — sprint completion:** `GET /sprints/{id}/completion-preview` → incomplete issues + completed points. `POST /sprints/{id}/complete` with `carryOverIssueIds` + `targetSprintId` → selected issues carry over, rest go to backlog, `completedPoints` snapshotted (see `/projects/{id}/velocity`).

**S3 — illegal transition:** `POST /issues/{id}/transitions` To Do → Done → **422 WORKFLOW_VIOLATION** with `details.allowedTransitions: ["In Progress"]`.

**S4 — notification outage:** `POST /api/v1/demo/notification-outage?enabled=true` → deliveries fail, circuit breaker (min 5 calls, 50% threshold) **opens**; board/API unaffected; notifications queue as `PENDING`. Set `enabled=false` → breaker half-opens, queue drains to `DELIVERED`.

## Load test

```bash
k6 run -e BASE_URL=http://localhost:8080 load-test/board-viewers.js
```
100 concurrent viewers, thresholds: p95 < 300ms, error rate < 1%.

## Docs
- `docs/erd.md` — ER diagram (mermaid)
- `docs/adr/` — six ADRs incl. trade-offs (storage choices, outbox, CQRS-lite, concurrency, WS replay, scaling)

## Trade-offs (summary; details in ADRs)
- **Postgres FTS over Elasticsearch**: one store, transactional indexing via generated columns; trades relevance tuning we don't need.
- **Outbox poller over Kafka**: same delivery guarantees at this scale, zero extra infra.
- **JPA entities with plain UUID FKs (no associations)**: prevents N+1/lazy-loading bugs by construction; joins happen explicitly at the query layer.
- **CQRS-lite, not event sourcing**: read model + cache gives the performance win without projection complexity.
