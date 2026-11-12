-- Fase F4: área da coordenação (análise, decisões, histórico e requerimento oficial).

-- 1) Quais cursos cada coordenador enxerga. ADMIN enxerga todos e não precisa de vínculo.
CREATE TABLE coordenacao (
    usuario_id UUID        NOT NULL REFERENCES usuario (id),
    curso_id   UUID        NOT NULL REFERENCES curso (id),
    criado_em  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (usuario_id, curso_id)
);

-- 2) Última análise da coordenação, para o aluno ver o motivo de um ajuste ou indeferimento.
ALTER TABLE certificado ADD COLUMN avaliado_em            TIMESTAMPTZ;
ALTER TABLE certificado ADD COLUMN avaliado_por           UUID REFERENCES usuario (id);
ALTER TABLE certificado ADD COLUMN observacao_coordenacao VARCHAR(1000);

-- 3) Histórico de decisões (só inserção): quem decidiu, o quê, por quê e o que mudou.
CREATE TABLE avaliacao (
    id               UUID PRIMARY KEY,
    certificado_id   UUID          NOT NULL REFERENCES certificado (id),
    avaliador_id     UUID          NOT NULL REFERENCES usuario (id),
    decisao          VARCHAR(30)   NOT NULL,
    justificativa    VARCHAR(1000),
    item_anterior    INTEGER,
    item_novo        INTEGER,
    horas_anteriores NUMERIC(6, 2),
    horas_novas      NUMERIC(6, 2),
    criado_em        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_avaliacao_decisao CHECK (decisao IN ('DEFERIDO', 'INDEFERIDO', 'AJUSTE_SOLICITADO', 'REABERTO'))
);

CREATE INDEX ix_avaliacao_certificado ON avaliacao (certificado_id, criado_em);

-- 4) Requerimentos oficiais gerados. As linhas são uma fotografia do momento: o documento não muda se um
--    certificado for reaberto depois.
CREATE TABLE requerimento (
    id               UUID PRIMARY KEY,
    aluno_id         UUID          NOT NULL REFERENCES aluno (id),
    numero           INTEGER       NOT NULL,
    nome_aluno       VARCHAR(200)  NOT NULL,
    matricula        VARCHAR(30)   NOT NULL,
    curso            VARCHAR(200)  NOT NULL,
    coordenador_id   UUID          NOT NULL REFERENCES usuario (id),
    coordenador_nome VARCHAR(200)  NOT NULL,
    total_horas      NUMERIC(7, 2) NOT NULL,
    gerado_em        TIMESTAMPTZ   NOT NULL,
    data_deferimento DATE          NOT NULL,
    CONSTRAINT uk_requerimento_numero UNIQUE (aluno_id, numero)
);

CREATE TABLE requerimento_linha (
    requerimento_id      UUID          NOT NULL REFERENCES requerimento (id),
    ordem                INTEGER       NOT NULL,
    item                 INTEGER       NOT NULL,
    macroatividade       VARCHAR(200)  NOT NULL,
    nome                 VARCHAR(300)  NOT NULL,
    descricao            TEXT,
    horas_por_ocorrencia NUMERIC(6, 2) NOT NULL,
    quantidade           INTEGER       NOT NULL,
    horas_integralizadas NUMERIC(7, 2) NOT NULL,
    PRIMARY KEY (requerimento_id, ordem)
);
