-- Adiciona índices para otimizar as consultas de Analytics (Heatmap e Streaks)
-- As queries frequentemente buscam por user_id via join com tasks e areas,
-- e filtram/ordenam por log_date.

-- Índice composto para otimizar a busca do histórico de uma tarefa específica.
CREATE INDEX IF NOT EXISTS idx_daily_logs_task_id_log_date ON daily_logs (task_id, log_date DESC);

-- Índice simples em log_date, útil para buscas de Heatmap que filtram logs
-- dentro de um range de datas específico.
CREATE INDEX IF NOT EXISTS idx_daily_logs_log_date ON daily_logs (log_date DESC);
