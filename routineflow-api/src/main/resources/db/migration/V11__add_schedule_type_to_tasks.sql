-- Allow day_of_week to be null (required for DAY_OF_MONTH tasks)
ALTER TABLE tasks ALTER COLUMN day_of_week DROP NOT NULL;

-- Add schedule type column (default DAY_OF_WEEK for backward compat)
ALTER TABLE tasks
ADD COLUMN schedule_type VARCHAR(15) NOT NULL DEFAULT 'DAY_OF_WEEK';

-- Add day_of_month column (1-31, nullable — only used when schedule_type = DAY_OF_MONTH)
ALTER TABLE tasks
ADD COLUMN day_of_month INT;

-- Constraint: schedule_type must be a known value
ALTER TABLE tasks
ADD CONSTRAINT chk_schedule_type
CHECK (schedule_type IN ('DAY_OF_WEEK', 'DAY_OF_MONTH'));

-- Constraint: day_of_month must be 1-31 if provided
ALTER TABLE tasks
ADD CONSTRAINT chk_day_of_month
CHECK (day_of_month IS NULL OR (day_of_month >= 1 AND day_of_month <= 31));

-- Constraint: consistency between schedule_type and fields
--   DAY_OF_WEEK  → day_of_week NOT NULL
--   DAY_OF_MONTH → day_of_month NOT NULL
ALTER TABLE tasks
ADD CONSTRAINT chk_schedule_consistency CHECK (
    (schedule_type = 'DAY_OF_WEEK'  AND day_of_week IS NOT NULL)
    OR
    (schedule_type = 'DAY_OF_MONTH' AND day_of_month IS NOT NULL)
);
