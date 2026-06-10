CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    display_name  VARCHAR(120) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE projects (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_key  VARCHAR(10) NOT NULL UNIQUE,
    name         VARCHAR(200) NOT NULL,
    description  TEXT,
    lead_id      UUID REFERENCES users(id),
    issue_seq    BIGINT NOT NULL DEFAULT 0,
    event_seq    BIGINT NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE project_members (
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role       VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN','PROJECT_LEAD','MEMBER','VIEWER')),
    added_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (project_id, user_id)
);
CREATE INDEX idx_members_user ON project_members(user_id);

CREATE TABLE workflow_statuses (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name       VARCHAR(60) NOT NULL,
    category   VARCHAR(15) NOT NULL CHECK (category IN ('TODO','IN_PROGRESS','DONE')),
    position   INT NOT NULL,
    wip_limit  INT,
    UNIQUE (project_id, name)
);

CREATE TABLE workflow_transitions (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id     UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    from_status_id UUID NOT NULL REFERENCES workflow_statuses(id) ON DELETE CASCADE,
    to_status_id   UUID NOT NULL REFERENCES workflow_statuses(id) ON DELETE CASCADE,
    name           VARCHAR(60),
    UNIQUE (project_id, from_status_id, to_status_id)
);

CREATE TABLE transition_hooks (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transition_id UUID NOT NULL REFERENCES workflow_transitions(id) ON DELETE CASCADE,
    kind          VARCHAR(10) NOT NULL CHECK (kind IN ('VALIDATOR','ACTION')),
    hook_type     VARCHAR(60) NOT NULL,
    config        JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE TABLE sprints (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id        UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name              VARCHAR(120) NOT NULL,
    goal              TEXT,
    status            VARCHAR(15) NOT NULL DEFAULT 'FUTURE' CHECK (status IN ('FUTURE','ACTIVE','COMPLETED')),
    start_date        DATE,
    end_date          DATE,
    completed_at      TIMESTAMPTZ,
    completed_points  INT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (start_date IS NULL OR end_date IS NULL OR end_date >= start_date)
);
CREATE INDEX idx_sprints_project ON sprints(project_id, status);

CREATE TABLE issues (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id   UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    issue_key    VARCHAR(20) NOT NULL UNIQUE,
    type         VARCHAR(10) NOT NULL CHECK (type IN ('EPIC','STORY','TASK','BUG','SUBTASK')),
    title        VARCHAR(300) NOT NULL,
    description  TEXT,
    status_id    UUID NOT NULL REFERENCES workflow_statuses(id),
    priority     VARCHAR(10) NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOWEST','LOW','MEDIUM','HIGH','HIGHEST')),
    version      BIGINT NOT NULL DEFAULT 0,
    assignee_id  UUID REFERENCES users(id),
    reporter_id  UUID NOT NULL REFERENCES users(id),
    sprint_id    UUID REFERENCES sprints(id) ON DELETE SET NULL,
    parent_id    UUID REFERENCES issues(id) ON DELETE SET NULL,
    story_points INT CHECK (story_points IS NULL OR story_points >= 0),
    labels       JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    search_vector TSVECTOR GENERATED ALWAYS AS (
        setweight(to_tsvector('english', coalesce(title, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(description, '')), 'B')
    ) STORED
);

CREATE INDEX idx_issues_board    ON issues(project_id, status_id);
CREATE INDEX idx_issues_sprint   ON issues(sprint_id) WHERE sprint_id IS NOT NULL;
CREATE INDEX idx_issues_parent   ON issues(parent_id) WHERE parent_id IS NOT NULL;
CREATE INDEX idx_issues_assignee ON issues(assignee_id) WHERE assignee_id IS NOT NULL;
CREATE INDEX idx_issues_fts      ON issues USING GIN (search_vector);

CREATE TABLE issue_watchers (
    issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (issue_id, user_id)
);

CREATE TABLE custom_field_definitions (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name       VARCHAR(80) NOT NULL,
    field_type VARCHAR(10) NOT NULL CHECK (field_type IN ('TEXT','NUMBER','DROPDOWN','DATE')),
    options    JSONB NOT NULL DEFAULT '[]'::jsonb,
    UNIQUE (project_id, name)
);

CREATE TABLE custom_field_values (
    issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    field_id UUID NOT NULL REFERENCES custom_field_definitions(id) ON DELETE CASCADE,
    value    JSONB NOT NULL,
    PRIMARY KEY (issue_id, field_id)
);

CREATE TABLE comments (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id          UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    parent_comment_id UUID REFERENCES comments(id) ON DELETE CASCADE,
    author_id         UUID NOT NULL REFERENCES users(id),
    body              TEXT NOT NULL,
    mentions          JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    search_vector     TSVECTOR GENERATED ALWAYS AS (to_tsvector('english', coalesce(body,''))) STORED
);
CREATE INDEX idx_comments_issue ON comments(issue_id, created_at);
CREATE INDEX idx_comments_fts   ON comments USING GIN (search_vector);

CREATE TABLE domain_events (
    id           BIGSERIAL PRIMARY KEY,
    project_id   UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    project_seq  BIGINT NOT NULL,
    event_type   VARCHAR(60) NOT NULL,
    aggregate_type VARCHAR(30) NOT NULL,
    aggregate_id UUID NOT NULL,
    actor_id     UUID REFERENCES users(id),
    payload      JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    dispatched_at TIMESTAMPTZ,
    UNIQUE (project_id, project_seq)
);
CREATE INDEX idx_events_feed     ON domain_events(project_id, id DESC);
CREATE INDEX idx_events_pending  ON domain_events(id) WHERE dispatched_at IS NULL;
CREATE INDEX idx_events_aggregate ON domain_events(aggregate_id, id DESC);

CREATE TABLE notifications (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type       VARCHAR(40) NOT NULL,
    payload    JSONB NOT NULL DEFAULT '{}'::jsonb,
    status     VARCHAR(15) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','DELIVERED','FAILED')),
    read_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user ON notifications(user_id, created_at DESC);
CREATE INDEX idx_notifications_pending ON notifications(created_at) WHERE status = 'PENDING';

CREATE TABLE idempotency_keys (
    key          VARCHAR(100) PRIMARY KEY,
    user_id      UUID NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_status INT,
    response_body   JSONB,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE security_audit_log (
    id         BIGSERIAL PRIMARY KEY,
    actor_id   UUID,
    action     VARCHAR(60) NOT NULL,
    target     VARCHAR(120),
    details    JSONB NOT NULL DEFAULT '{}'::jsonb,
    ip_address VARCHAR(45),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
