-- V3: Índice em users(email) para otimizar a query de login
-- A V1 já criou idx_users_email, mas esta migration garante existência
-- mesmo em ambientes que não rodaram a V1 com o índice

-- Sem-op: índice já existe via V1. Migration vazia para manter sequência de versões.
-- Se precisar adicionar outro índice de login no futuro, usar esta versão.
SELECT 1;
