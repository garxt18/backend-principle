-- =====================================================================
-- V2: the AI mentor is removed, the DSA sheet and personal line notes
-- are added, and Planly plans get a weekly DSA target.
-- (Never edit V1 - Flyway has already applied it on existing databases.)
-- =====================================================================

DROP TABLE IF EXISTS ai_usage;
DROP TABLE IF EXISTS ai_explanations;

-- ---------------------------------------------------------------------
-- DSA sheet (shared catalog, seeded from dsa/dsa-sheet.json)
-- ---------------------------------------------------------------------
CREATE TABLE dsa_topics (
    id          BIGSERIAL PRIMARY KEY,
    slug        VARCHAR(80)  NOT NULL UNIQUE,
    title       VARCHAR(120) NOT NULL,
    summary     TEXT,
    order_index INT          NOT NULL
);

CREATE TABLE dsa_problems (
    id          BIGSERIAL PRIMARY KEY,
    topic_id    BIGINT       NOT NULL REFERENCES dsa_topics (id) ON DELETE CASCADE,
    slug        VARCHAR(120) NOT NULL UNIQUE,
    title       VARCHAR(160) NOT NULL,
    difficulty  VARCHAR(10)  NOT NULL CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    practice_url VARCHAR(500) NOT NULL,
    order_index INT          NOT NULL
);
CREATE INDEX ix_dsa_problems_topic ON dsa_problems (topic_id, order_index);

CREATE TABLE dsa_topic_resources (
    id          BIGSERIAL PRIMARY KEY,
    topic_id    BIGINT        NOT NULL REFERENCES dsa_topics (id) ON DELETE CASCADE,
    title       VARCHAR(200)  NOT NULL,
    url         VARCHAR(1000) NOT NULL,
    channel     VARCHAR(120),
    language    VARCHAR(10)   NOT NULL,
    order_index INT           NOT NULL
);

-- Per-user status of each problem (TUF-style: solved checkbox, revision star, notes).
CREATE TABLE dsa_progress (
    id          BIGSERIAL PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    problem_id  BIGINT      NOT NULL REFERENCES dsa_problems (id) ON DELETE CASCADE,
    solved      BOOLEAN     NOT NULL DEFAULT FALSE,
    revision    BOOLEAN     NOT NULL DEFAULT FALSE,
    notes       TEXT,
    solved_at   TIMESTAMPTZ,
    updated_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT ux_dsa_progress UNIQUE (user_id, problem_id)
);
CREATE INDEX ix_dsa_progress_solved ON dsa_progress (user_id, solved_at) WHERE solved;

-- ---------------------------------------------------------------------
-- Planly: how many DSA problems the learner wants to solve per week
-- ---------------------------------------------------------------------
ALTER TABLE study_plans ADD COLUMN dsa_per_week INT NOT NULL DEFAULT 0
    CHECK (dsa_per_week BETWEEN 0 AND 70);

-- ---------------------------------------------------------------------
-- Rebuild Lab: learners explain lines in their own words
-- ---------------------------------------------------------------------
CREATE TABLE lab_line_notes (
    id          BIGSERIAL PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    file_id     UUID        NOT NULL REFERENCES lab_files (id) ON DELETE CASCADE,
    line_number INT         NOT NULL CHECK (line_number > 0),
    note        TEXT        NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT ux_lab_line_notes UNIQUE (user_id, file_id, line_number)
);
