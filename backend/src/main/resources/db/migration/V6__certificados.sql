CREATE TABLE arquivo (
    id            UUID PRIMARY KEY,
    chave         VARCHAR(200) NOT NULL,          -- localização no armazenamento; nunca o nome original
    nome_original VARCHAR(255) NOT NULL,
    tipo_mime     VARCHAR(100) NOT NULL,
    tamanho_bytes BIGINT       NOT NULL,
    sha256        VARCHAR(64)  NOT NULL,
    enviado_por   UUID         NOT NULL REFERENCES usuario (id),
    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_arquivo_chave UNIQUE (chave)
);

CREATE INDEX ix_arquivo_sha256 ON arquivo (enviado_por, sha256);

CREATE TABLE certificado (
    id                   UUID PRIMARY KEY,
    aluno_id             UUID         NOT NULL REFERENCES aluno (id),
    arquivo_id           UUID         NOT NULL REFERENCES arquivo (id),
    regulamento_id       UUID         NOT NULL,
    item_numero          INTEGER      NOT NULL,
    status               VARCHAR(30)  NOT NULL,
    titulo               VARCHAR(300) NOT NULL,
    instituicao          VARCHAR(200),
    data_inicio          DATE,
    data_fim             DATE,
    horas_certificado    NUMERIC(6, 2) NOT NULL,
    origem_classificacao VARCHAR(20)  NOT NULL,
    confirmado_em        TIMESTAMPTZ,
    excluido_em          TIMESTAMPTZ,
    versao               BIGINT       NOT NULL DEFAULT 0,
    criado_em            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_certificado_item FOREIGN KEY (regulamento_id, item_numero)
        REFERENCES item_atividade (regulamento_id, numero),
    CONSTRAINT ck_certificado_status CHECK (status IN
        ('AGUARDANDO_CONFIRMACAO', 'CONFIRMADO', 'AJUSTE_SOLICITADO', 'DEFERIDO', 'INDEFERIDO')),
    CONSTRAINT ck_certificado_origem CHECK (origem_classificacao IN ('MANUAL', 'REGRA', 'IA', 'IMPORTACAO')),
    CONSTRAINT ck_certificado_horas CHECK (horas_certificado >= 0),
    CONSTRAINT ck_certificado_datas CHECK (data_fim IS NULL OR data_inicio IS NULL OR data_fim >= data_inicio)
);

CREATE INDEX ix_certificado_aluno ON certificado (aluno_id, status) WHERE excluido_em IS NULL;
CREATE INDEX ix_certificado_fila ON certificado (status, criado_em) WHERE excluido_em IS NULL;
