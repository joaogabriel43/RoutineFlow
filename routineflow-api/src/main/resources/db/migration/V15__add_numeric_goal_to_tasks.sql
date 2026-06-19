ALTER TABLE tasks
ADD COLUMN goal_type VARCHAR(15) NOT NULL DEFAULT 'BOOLEAN';

ALTER TABLE tasks
ADD COLUMN goal_target DECIMAL(10,2);

ALTER TABLE tasks
ADD COLUMN goal_unit VARCHAR(30);

ALTER TABLE daily_logs
ADD COLUMN goal_progress DECIMAL(10,2);

ALTER TABLE tasks
ADD CONSTRAINT chk_goal_type
CHECK (goal_type IN ('BOOLEAN', 'NUMERIC'));

ALTER TABLE tasks
ADD CONSTRAINT chk_goal_consistency CHECK (
    (goal_type = 'BOOLEAN' AND goal_target IS NULL)
    OR
    (goal_type = 'NUMERIC' AND goal_target IS NOT NULL AND goal_target > 0)
);
