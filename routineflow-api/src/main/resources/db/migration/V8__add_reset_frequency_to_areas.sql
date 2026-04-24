ALTER TABLE areas
ADD COLUMN reset_frequency VARCHAR(10) NOT NULL DEFAULT 'DAILY';

ALTER TABLE areas
ADD CONSTRAINT chk_reset_frequency
CHECK (reset_frequency IN ('DAILY', 'WEEKLY', 'MONTHLY'));
