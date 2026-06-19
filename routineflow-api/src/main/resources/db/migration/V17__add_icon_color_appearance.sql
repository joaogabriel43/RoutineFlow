-- V17: Sprint 25 — custom appearance (lucide icon name + hex color)
--
-- Findings (schema audit before this migration):
--   areas.color  VARCHAR(7)  NOT NULL  → already exists, serves hex (#RRGGBB)
--   areas.icon   VARCHAR(10) NOT NULL  → already exists, held an emoji
--   tasks        → had NO icon / color columns
--
-- areas.icon is widened to hold lucide kebab-case names (e.g. "alert-triangle",
-- "message-circle") which do not fit in 10 chars. Existing emoji values still fit.
ALTER TABLE areas ALTER COLUMN icon TYPE VARCHAR(50);

-- tasks gain optional appearance. Nullable — existing tasks keep working (null →
-- UI applies a default icon/color). icon: lucide kebab-case. color: hex "#RRGGBB".
ALTER TABLE tasks ADD COLUMN icon VARCHAR(50);
ALTER TABLE tasks ADD COLUMN color VARCHAR(7);
