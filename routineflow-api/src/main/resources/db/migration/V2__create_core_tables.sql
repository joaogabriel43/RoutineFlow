-- V2: Tabelas principais do domínio (areas, tasks, daily_logs, streaks)

CREATE TABLE areas (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name       VARCHAR(255) NOT NULL,
    color      VARCHAR(7)   NOT NULL,
    icon       VARCHAR(10)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_areas_user_id ON areas (user_id);

-- ------------------------------------------------------------

CREATE TABLE tasks (
    id                BIGSERIAL PRIMARY KEY,
    area_id           BIGINT       NOT NULL REFERENCES areas (id) ON DELETE CASCADE,
    title             VARCHAR(255) NOT NULL,
    description       TEXT,
    estimated_minutes INT,
    day_of_week       VARCHAR(10)  NOT NULL,
    order_index       INT          NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tasks_area_id ON tasks (area_id);

-- ------------------------------------------------------------

CREATE TABLE daily_logs (
    id           BIGSERIAL PRIMARY KEY,
    task_id      BIGINT  NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    user_id      BIGINT  NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    log_date     DATE    NOT NULL,
    completed    BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMPTZ,

    CONSTRAINT uq_daily_logs_task_user_date UNIQUE (task_id, user_id, log_date)
);

-- Índice principal do streak engine e do heatmap (consultas por usuário + data)
CREATE INDEX idx_daily_logs_user_date ON daily_logs (user_id, log_date);

-- ------------------------------------------------------------

CREATE TABLE streaks (
    id               BIGSERIAL PRIMARY KEY,
    area_id          BIGINT NOT NULL REFERENCES areas (id) ON DELETE CASCADE,
    user_id          BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    current_count    INT    NOT NULL DEFAULT 0,
    last_active_date DATE,

    CONSTRAINT uq_streaks_area_user UNIQUE (area_id, user_id)
);

-- Índice para busca de streaks por usuário
CREATE INDEX idx_streaks_user_area ON streaks (user_id, area_id);
