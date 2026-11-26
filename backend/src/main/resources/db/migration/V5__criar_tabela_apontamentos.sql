-- Horas trabalhadas em cada tarefa. A duração fica em minutos para não perder precisão com frações de hora.
-- Excluir a tarefa (ou o quadro, ou o projeto) exclui junto as horas apontadas nela.
CREATE TABLE apontamentos (
    id            BIGSERIAL    PRIMARY KEY,
    tarefa_id     BIGINT       NOT NULL REFERENCES tarefas (id) ON DELETE CASCADE,
    usuario_id    BIGINT       NOT NULL REFERENCES usuarios (id),
    data          DATE         NOT NULL,
    minutos       INTEGER      NOT NULL,
    descricao     VARCHAR(500),
    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_apontamentos_minutos CHECK (minutos > 0 AND minutos <= 1440)
);

CREATE INDEX idx_apontamentos_usuario_data ON apontamentos (usuario_id, data);
CREATE INDEX idx_apontamentos_tarefa ON apontamentos (tarefa_id);
CREATE INDEX idx_apontamentos_data ON apontamentos (data);
