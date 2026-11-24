-- Refresh tokens emitidos no login. Guardamos só o hash SHA-256 do token,
-- nunca o valor original, e cada token é de uso único (rotação a cada refresh).
CREATE TABLE refresh_tokens (
    id          BIGSERIAL    PRIMARY KEY,
    usuario_id  BIGINT       NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL,
    expira_em   TIMESTAMPTZ  NOT NULL,
    revogado    BOOLEAN      NOT NULL DEFAULT FALSE,
    criado_em   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_usuario ON refresh_tokens (usuario_id);
