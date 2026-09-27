-- =====================================================================
-- V4: Planly plans become day-by-day. A plan is created either from a
-- pace (hours per day on chosen weekdays) or from a deadline ("finish
-- Java Basics in 7 days"), for any set of levels or topics.
-- =====================================================================

ALTER TABLE study_plans
    ADD COLUMN name            VARCHAR(120),
    ADD COLUMN pace_mode       VARCHAR(12)  NOT NULL DEFAULT 'HOURS' CHECK (pace_mode IN ('HOURS', 'DEADLINE')),
    ADD COLUMN hours_per_day   NUMERIC(4,2),
    ADD COLUMN study_days      VARCHAR(20)  NOT NULL DEFAULT '1,2,3,4,5,6',
    ADD COLUMN target_end_date DATE,
    ADD COLUMN end_date        DATE;

-- A deadline plan can ask for more than 80 h/week (e.g. 10 h/day for a 5-day sprint).
ALTER TABLE study_plans DROP CONSTRAINT IF EXISTS study_plans_hours_per_week_check;
ALTER TABLE study_plans ADD CONSTRAINT study_plans_hours_per_week_check CHECK (hours_per_week BETWEEN 1 AND 168);

-- Existing weekly plans: 6 study days a week.
UPDATE study_plans
SET hours_per_day = GREATEST(0.5, ROUND(hours_per_week / 6.0, 2)),
    end_date      = start_date + (total_weeks * 7) - 1;
ALTER TABLE study_plans ALTER COLUMN hours_per_day SET NOT NULL;
ALTER TABLE study_plans ALTER COLUMN end_date SET NOT NULL;

-- Every plan item gets its own day; hours can now be fractions like 0.75 (45 minutes).
ALTER TABLE plan_items ADD COLUMN planned_date DATE;
UPDATE plan_items pi
SET planned_date = sp.start_date + (pi.week_number - 1) * 7
FROM study_plans sp
WHERE sp.id = pi.plan_id;
ALTER TABLE plan_items ALTER COLUMN planned_date SET NOT NULL;
ALTER TABLE plan_items ALTER COLUMN planned_hours TYPE NUMERIC(6,2);
CREATE INDEX ix_plan_items_plan_date ON plan_items (plan_id, planned_date, order_index);
