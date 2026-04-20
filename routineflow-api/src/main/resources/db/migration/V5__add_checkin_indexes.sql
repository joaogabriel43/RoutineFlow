-- V5: Índices para queries de check-in e progresso diário
-- O UNIQUE constraint (task_id, user_id, log_date) já foi criado no V2.
-- Aqui adicionamos índices de suporte para as queries de leitura mais frequentes.

-- Cobre: findAllByUserIdAndLogDate (dashboard de progresso diário)
-- O idx_daily_logs_user_date já existe no V2 — confirma existência e adiciona variante
CREATE INDEX IF NOT EXISTS idx_daily_logs_user_date_completed
    ON daily_logs (user_id, log_date, completed);

-- Cobre: findCompletedByUserIdAndAreaIdAndLogDate (streak engine)
CREATE INDEX IF NOT EXISTS idx_daily_logs_task_user_date
    ON daily_logs (task_id, user_id, log_date);
