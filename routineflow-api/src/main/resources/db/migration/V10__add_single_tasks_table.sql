CREATE TABLE single_tasks (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL REFERENCES users(id),
    title         VARCHAR(255) NOT NULL,
    description   TEXT,
    due_date      DATE,
    completed     BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    archived_at   TIMESTAMPTZ
);

CREATE INDEX idx_single_tasks_user_id ON single_tasks(user_id);
CREATE INDEX idx_single_tasks_user_pending
    ON single_tasks(user_id, completed)
    WHERE completed = FALSE;
