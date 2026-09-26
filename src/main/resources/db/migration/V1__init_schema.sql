-- =====================================================================
-- Backend Playground - initial schema
-- Every table that belongs to a person carries user_id so one database
-- can safely serve many learners (multi-tenant by row ownership).
-- =====================================================================

CREATE TABLE users (
    id                 UUID PRIMARY KEY,
    email              VARCHAR(254) NOT NULL,
    password_hash      VARCHAR(100) NOT NULL,
    display_name       VARCHAR(80)  NOT NULL,
    role               VARCHAR(20)  NOT NULL,
    preferred_language VARCHAR(10)  NOT NULL DEFAULT 'BOTH',
    enabled            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL,
    version            BIGINT       NOT NULL DEFAULT 0
);
-- Emails are compared case-insensitively: store lower-case and enforce uniqueness.
CREATE UNIQUE INDEX ux_users_email ON users (email);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL,
    family_id   UUID        NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL
);
CREATE UNIQUE INDEX ux_refresh_tokens_hash ON refresh_tokens (token_hash);
CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX ix_refresh_tokens_family ON refresh_tokens (family_id);

-- ---------------------------------------------------------------------
-- Roadmap catalog (shared by all users, seeded from roadmap/roadmap.json)
-- ---------------------------------------------------------------------
CREATE TABLE roadmap_levels (
    id                  BIGSERIAL PRIMARY KEY,
    slug                VARCHAR(80)  NOT NULL UNIQUE,
    level_number        INT          NOT NULL UNIQUE,
    title               VARCHAR(120) NOT NULL,
    summary             TEXT         NOT NULL,
    why_it_matters      TEXT,
    project_title       VARCHAR(160),
    project_description TEXT,
    suggested_month     INT
);

CREATE TABLE topics (
    id              BIGSERIAL PRIMARY KEY,
    level_id        BIGINT       NOT NULL REFERENCES roadmap_levels (id) ON DELETE CASCADE,
    slug            VARCHAR(120) NOT NULL UNIQUE,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    practice        TEXT,
    estimated_hours INT          NOT NULL CHECK (estimated_hours > 0),
    order_index     INT          NOT NULL
);
CREATE INDEX ix_topics_level ON topics (level_id, order_index);

CREATE TABLE resources (
    id          BIGSERIAL PRIMARY KEY,
    level_id    BIGINT        NOT NULL REFERENCES roadmap_levels (id) ON DELETE CASCADE,
    title       VARCHAR(200)  NOT NULL,
    url         VARCHAR(1000) NOT NULL,
    channel     VARCHAR(120),
    language    VARCHAR(10)   NOT NULL,
    kind        VARCHAR(20)   NOT NULL,
    primary_pick BOOLEAN      NOT NULL DEFAULT FALSE,
    note        VARCHAR(500),
    order_index INT           NOT NULL DEFAULT 0
);
CREATE INDEX ix_resources_level ON resources (level_id, order_index);

-- ---------------------------------------------------------------------
-- Per-user learning progress
-- ---------------------------------------------------------------------
CREATE TABLE topic_progress (
    id           BIGSERIAL PRIMARY KEY,
    user_id      UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    topic_id     BIGINT      NOT NULL REFERENCES topics (id) ON DELETE CASCADE,
    status       VARCHAR(20) NOT NULL,
    confidence   SMALLINT CHECK (confidence BETWEEN 1 AND 5),
    notes        TEXT,
    started_at   TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    updated_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT ux_topic_progress UNIQUE (user_id, topic_id)
);

CREATE TABLE study_plans (
    id             UUID PRIMARY KEY,
    user_id        UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    start_date     DATE        NOT NULL,
    hours_per_week INT         NOT NULL CHECK (hours_per_week BETWEEN 1 AND 80),
    total_weeks    INT         NOT NULL,
    status         VARCHAR(20) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL
);
-- A learner has at most one ACTIVE plan; older plans are kept as ARCHIVED history.
CREATE UNIQUE INDEX ux_study_plans_one_active ON study_plans (user_id) WHERE status = 'ACTIVE';

CREATE TABLE plan_items (
    id            BIGSERIAL PRIMARY KEY,
    plan_id       UUID         NOT NULL REFERENCES study_plans (id) ON DELETE CASCADE,
    topic_id      BIGINT       NOT NULL REFERENCES topics (id) ON DELETE CASCADE,
    week_number   INT          NOT NULL,
    order_index   INT          NOT NULL,
    planned_hours NUMERIC(5,1) NOT NULL
);
CREATE INDEX ix_plan_items_plan_week ON plan_items (plan_id, week_number, order_index);

CREATE TABLE study_sessions (
    id           BIGSERIAL PRIMARY KEY,
    user_id      UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    topic_id     BIGINT       REFERENCES topics (id) ON DELETE SET NULL,
    session_date DATE         NOT NULL,
    minutes      INT          NOT NULL CHECK (minutes BETWEEN 1 AND 720),
    note         VARCHAR(500),
    created_at   TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_study_sessions_user_date ON study_sessions (user_id, session_date);

-- ---------------------------------------------------------------------
-- Rebuild Lab: projects you retype line by line
-- owner_id NULL means a shared template visible to everyone.
-- ---------------------------------------------------------------------
CREATE TABLE lab_projects (
    id          UUID PRIMARY KEY,
    owner_id    UUID         REFERENCES users (id) ON DELETE CASCADE,
    slug        VARCHAR(80),
    name        VARCHAR(120) NOT NULL,
    description VARCHAR(1000),
    source_kind VARCHAR(20)  NOT NULL,
    file_count  INT          NOT NULL,
    total_lines INT          NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_lab_projects_owner ON lab_projects (owner_id);
CREATE UNIQUE INDEX ux_lab_projects_template_slug ON lab_projects (slug) WHERE owner_id IS NULL;

CREATE TABLE lab_files (
    id           UUID PRIMARY KEY,
    project_id   UUID         NOT NULL REFERENCES lab_projects (id) ON DELETE CASCADE,
    path         VARCHAR(500) NOT NULL,
    language     VARCHAR(20)  NOT NULL,
    layer        VARCHAR(20)  NOT NULL,
    build_order  INT          NOT NULL,
    line_count   INT          NOT NULL,
    content      TEXT         NOT NULL,
    CONSTRAINT ux_lab_files_path UNIQUE (project_id, path)
);
CREATE INDEX ix_lab_files_order ON lab_files (project_id, build_order);

CREATE TABLE lab_file_progress (
    id              BIGSERIAL PRIMARY KEY,
    user_id         UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    file_id         UUID        NOT NULL REFERENCES lab_files (id) ON DELETE CASCADE,
    lines_completed INT         NOT NULL DEFAULT 0,
    completed       BOOLEAN     NOT NULL DEFAULT FALSE,
    updated_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT ux_lab_file_progress UNIQUE (user_id, file_id)
);

-- AI answers are expensive: cache them by a hash of (prompt kind + code + context)
-- so the second learner who asks about the same line gets it for free.
CREATE TABLE ai_explanations (
    id           BIGSERIAL PRIMARY KEY,
    cache_key    VARCHAR(64) NOT NULL UNIQUE,
    explanation  TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL
);

-- Per-user, per-day AI request counter used to enforce a fair-use quota.
CREATE TABLE ai_usage (
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    usage_date DATE NOT NULL,
    requests   INT  NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, usage_date)
);
