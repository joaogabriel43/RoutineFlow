ALTER TABLE streaks
    ADD COLUMN best_streak INT NOT NULL DEFAULT 0;

-- Initialize best_streak with the current count for all existing rows that have progress
UPDATE streaks SET best_streak = current_count WHERE current_count > 0;
