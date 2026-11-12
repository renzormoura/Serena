-- Regulamento de atividades complementares, versionado.
-- Cada aluno será vinculado a uma versão (ver PLANO-DO-PROJETO.md §8 e §9).

CREATE TABLE regulamento (
    id             UUID PRIMARY KEY,
    nome           VARCHAR(200) NOT NULL,
    versao         INTEGER      NOT NULL,
    vigente_desde  DATE,
    vigente_ate    DATE,
    horas_exigidas NUMERIC(6, 2),            -- NULL enquanto a coordenação não definir (decisão D1)
    publicado      BOOLEAN      NOT NULL DEFAULT FALSE,
    criado_em      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_regulamento_nome_versao UNIQUE (nome, versao),
    CONSTRAINT ck_regulamento_horas CHECK (horas_exigidas IS NULL OR horas_exigidas > 0)
);

CREATE TABLE macroatividade (
    id             UUID PRIMARY KEY,
    regulamento_id UUID         NOT NULL REFERENCES regulamento (id),
    codigo         VARCHAR(60)  NOT NULL,
    ordem          INTEGER      NOT NULL,
    nome           VARCHAR(200) NOT NULL,
    limite_horas   NUMERIC(6, 2),
    CONSTRAINT uk_macro_codigo UNIQUE (regulamento_id, codigo),
    CONSTRAINT ck_macro_limite CHECK (limite_horas IS NULL OR limite_horas > 0)
);

CREATE TABLE item_atividade (
    id                   UUID PRIMARY KEY,
    regulamento_id       UUID         NOT NULL REFERENCES regulamento (id),
    macroatividade_id    UUID         NOT NULL REFERENCES macroatividade (id),
    numero               INTEGER      NOT NULL,
    nome                 VARCHAR(200) NOT NULL,
    descricao            TEXT,
    horas_por_ocorrencia NUMERIC(6, 2) NOT NULL,
    estrategia           VARCHAR(40)  NOT NULL,
    teto_por_ocorrencia  NUMERIC(6, 2),
    limite_ocorrencias   INTEGER,
    limite_horas         NUMERIC(6, 2),
    palavras_chave       TEXT[]       NOT NULL DEFAULT '{}',
    ativo                BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_item_numero UNIQUE (regulamento_id, numero),
    CONSTRAINT ck_item_estrategia CHECK (estrategia IN ('FIXO_POR_OCORRENCIA', 'HORAS_CERTIFICADO', 'HORAS_CERTIFICADO_COM_TETO')),
    CONSTRAINT ck_item_teto CHECK (estrategia <> 'HORAS_CERTIFICADO_COM_TETO' OR teto_por_ocorrencia IS NOT NULL),
    CONSTRAINT ck_item_horas CHECK (horas_por_ocorrencia >= 0)
);

CREATE INDEX ix_item_macro ON item_atividade (macroatividade_id);
