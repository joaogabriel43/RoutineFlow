-- V6: Tabela de auditoria de execuções do scheduler

CREATE TABLE scheduler_runs (
    id              BIGSERIAL    PRIMARY KEY,
    job_name        VARCHAR(100) NOT NULL,
    started_at      TIMESTAMPTZ  NOT NULL,
    finished_at     TIMESTAMPTZ,
    users_processed INT,
    status          VARCHAR(20)  NOT NULL,
    error_details   TEXT
);

-- Índice para consulta por job e data (relatórios de auditoria)
CREATE INDEX idx_scheduler_runs_job_started ON scheduler_runs (job_name, started_at DESC);
