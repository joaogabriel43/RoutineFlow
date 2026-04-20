-- V7: Add order_index to areas for drag-and-drop reordering in ManagePage

ALTER TABLE areas ADD COLUMN order_index INT NOT NULL DEFAULT 0;

CREATE INDEX idx_areas_routine_order ON areas (routine_id, order_index);
