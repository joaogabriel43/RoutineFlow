-- V4: Tabela de rotinas e vínculo com áreas

CREATE TABLE routines (
    id          BIGSERIAL   PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    active      BOOLEAN     NOT NULL DEFAULT TRUE,
    imported_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Índice para busca da rotina ativa do usuário (query mais frequente)
CREATE INDEX idx_routines_user_active ON routines (user_id, active);

-- Vínculo entre área e rotina (nullable para não quebrar dados do V2)
ALTER TABLE areas ADD COLUMN routine_id BIGINT REFERENCES routines (id) ON DELETE CASCADE;

CREATE INDEX idx_areas_routine_id ON areas (routine_id);
