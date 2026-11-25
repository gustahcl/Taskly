-- Projetos e seus membros. O papel no projeto (GESTOR ou COLABORADOR) é independente
-- do perfil global do usuário: o gestor edita o projeto e administra a equipe.
CREATE TABLE projetos (
    id            BIGSERIAL     PRIMARY KEY,
    nome          VARCHAR(120)  NOT NULL,
    descricao     VARCHAR(2000),
    cliente       VARCHAR(120),
    data_inicio   DATE,
    data_fim      DATE,
    status        VARCHAR(20)   NOT NULL DEFAULT 'PLANEJADO',
    criado_por    BIGINT        NOT NULL REFERENCES usuarios (id),
    criado_em     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT ck_projetos_status CHECK (status IN ('PLANEJADO', 'EM_ANDAMENTO', 'PAUSADO', 'CONCLUIDO', 'CANCELADO')),
    CONSTRAINT ck_projetos_datas CHECK (data_fim IS NULL OR data_inicio IS NULL OR data_fim >= data_inicio)
);

CREATE INDEX idx_projetos_status ON projetos (status);

CREATE TABLE projeto_membros (
    id            BIGSERIAL   PRIMARY KEY,
    projeto_id    BIGINT      NOT NULL REFERENCES projetos (id) ON DELETE CASCADE,
    usuario_id    BIGINT      NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    papel         VARCHAR(20) NOT NULL DEFAULT 'COLABORADOR',
    adicionado_em TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_projeto_membros UNIQUE (projeto_id, usuario_id),
    CONSTRAINT ck_projeto_membros_papel CHECK (papel IN ('GESTOR', 'COLABORADOR'))
);

CREATE INDEX idx_projeto_membros_usuario ON projeto_membros (usuario_id);
