CREATE TABLE skip_days (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    area_id BIGINT NOT NULL REFERENCES areas(id) ON DELETE CASCADE,
    skip_date DATE NOT NULL,
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, area_id, skip_date)
);

CREATE INDEX idx_skip_days_user_area ON skip_days(user_id, area_id);
