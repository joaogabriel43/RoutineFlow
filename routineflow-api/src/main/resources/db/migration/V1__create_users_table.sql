-- V1: Criação da tabela de usuários
-- Tabela central de autenticação — referenciada por todas as entidades do domínio

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Índice para login por email (operação mais frequente)
CREATE INDEX idx_users_email ON users (email);
