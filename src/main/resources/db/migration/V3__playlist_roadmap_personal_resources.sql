-- =====================================================================
-- V3: the DSA sheet is removed (learners follow Striver's A2Z sheet on
-- takeUforward directly), the Java and Spring Boot levels become
-- lecture-by-lecture playlist tracks, and every learner can pick the
-- resource they follow and attach their own links.
-- (Never edit V1/V2 - Flyway has already applied them.)
-- =====================================================================

DROP TABLE IF EXISTS dsa_progress;
DROP TABLE IF EXISTS dsa_topic_resources;
DROP TABLE IF EXISTS dsa_problems;
DROP TABLE IF EXISTS dsa_topics;
ALTER TABLE study_plans DROP COLUMN IF EXISTS dsa_per_week;

-- ---------------------------------------------------------------------
-- Playlist levels: a level can be taught by one playlist, and each of
-- its topics is one lecture of that playlist.
-- ---------------------------------------------------------------------
ALTER TABLE roadmap_levels
    ADD COLUMN playlist_name    VARCHAR(160),
    ADD COLUMN playlist_channel VARCHAR(120),
    ADD COLUMN playlist_url     VARCHAR(500);

ALTER TABLE topics
    ADD COLUMN lecture_number INT CHECK (lecture_number > 0),
    ADD COLUMN video_url      VARCHAR(500);

-- ---------------------------------------------------------------------
-- Resources: seeded rows are kept in sync with roadmap.json on every
-- start; rows an admin adds through the API (seeded = false) are never
-- touched by the seeder. YouTube search links are gone for good.
-- ---------------------------------------------------------------------
ALTER TABLE resources ADD COLUMN seeded BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE resources SET seeded = TRUE;
DELETE FROM resources WHERE kind = 'SEARCH';

-- A learner's own links (a playlist they prefer, a better video for one lecture...).
-- topic_id NULL = attached to the whole level.
CREATE TABLE user_resources (
    id         BIGSERIAL PRIMARY KEY,
    user_id    UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    level_id   BIGINT        NOT NULL REFERENCES roadmap_levels (id) ON DELETE CASCADE,
    topic_id   BIGINT        REFERENCES topics (id) ON DELETE CASCADE,
    title      VARCHAR(200)  NOT NULL,
    url        VARCHAR(1000) NOT NULL,
    note       VARCHAR(500),
    created_at TIMESTAMPTZ   NOT NULL
);
CREATE INDEX ix_user_resources_user ON user_resources (user_id, level_id);

-- "This is the one I'm going to follow" - at most one per learner per level,
-- pointing at either a catalog resource or one of the learner's own links.
CREATE TABLE resource_choices (
    id               BIGSERIAL PRIMARY KEY,
    user_id          UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    level_id         BIGINT      NOT NULL REFERENCES roadmap_levels (id) ON DELETE CASCADE,
    resource_id      BIGINT      REFERENCES resources (id) ON DELETE CASCADE,
    user_resource_id BIGINT      REFERENCES user_resources (id) ON DELETE CASCADE,
    chosen_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT ux_resource_choices UNIQUE (user_id, level_id),
    CONSTRAINT ck_resource_choices_one CHECK ((resource_id IS NULL) <> (user_resource_id IS NULL))
);
