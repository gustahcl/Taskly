-- Quadros kanban de um projeto, suas colunas e tarefas, e os comentários das tarefas.
-- A ordem das colunas e das tarefas dentro de cada coluna fica em "posicao" (0, 1, 2...).
CREATE TABLE quadros (
    id            BIGSERIAL    PRIMARY KEY,
    projeto_id    BIGINT       NOT NULL REFERENCES projetos (id) ON DELETE CASCADE,
    nome          VARCHAR(120) NOT NULL,
    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_quadros_projeto ON quadros (projeto_id);

CREATE TABLE colunas (
    id        BIGSERIAL   PRIMARY KEY,
    quadro_id BIGINT      NOT NULL REFERENCES quadros (id) ON DELETE CASCADE,
    nome      VARCHAR(60) NOT NULL,
    posicao   INTEGER     NOT NULL,

    CONSTRAINT ck_colunas_posicao CHECK (posicao >= 0)
);

CREATE INDEX idx_colunas_quadro ON colunas (quadro_id, posicao);

CREATE TABLE tarefas (
    id             BIGSERIAL     PRIMARY KEY,
    coluna_id      BIGINT        NOT NULL REFERENCES colunas (id) ON DELETE CASCADE,
    titulo         VARCHAR(200)  NOT NULL,
    descricao      VARCHAR(5000),
    responsavel_id BIGINT        REFERENCES usuarios (id) ON DELETE SET NULL,
    prioridade     VARCHAR(20)   NOT NULL DEFAULT 'MEDIA',
    prazo          DATE,
    posicao        INTEGER       NOT NULL,
    criado_por     BIGINT        NOT NULL REFERENCES usuarios (id),
    criado_em      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    atualizado_em  TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT ck_tarefas_prioridade CHECK (prioridade IN ('BAIXA', 'MEDIA', 'ALTA', 'URGENTE')),
    CONSTRAINT ck_tarefas_posicao CHECK (posicao >= 0)
);

CREATE INDEX idx_tarefas_coluna ON tarefas (coluna_id, posicao);
CREATE INDEX idx_tarefas_responsavel ON tarefas (responsavel_id);

CREATE TABLE comentarios (
    id        BIGSERIAL     PRIMARY KEY,
    tarefa_id BIGINT        NOT NULL REFERENCES tarefas (id) ON DELETE CASCADE,
    autor_id  BIGINT        NOT NULL REFERENCES usuarios (id),
    texto     VARCHAR(2000) NOT NULL,
    criado_em TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_comentarios_tarefa ON comentarios (tarefa_id, criado_em);
