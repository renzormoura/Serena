-- Fase F2: envio do arquivo primeiro, leitura automática em segundo plano, conferência pelo aluno depois.

-- 1) Registro de eventos do Spring Modulith (copiado de spring-modulith-events-jdbc 2.1.1, schemas/v2).
--    Garante que a leitura de um certificado não se perde se a aplicação cair no meio.
CREATE TABLE event_publication
(
    id                     UUID NOT NULL,
    listener_id            TEXT NOT NULL,
    event_type             TEXT NOT NULL,
    serialized_event       TEXT NOT NULL,
    publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date        TIMESTAMP WITH TIME ZONE,
    status                 TEXT,
    completion_attempts    INT,
    last_resubmission_date TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id)
);
CREATE INDEX event_publication_serialized_event_hash_idx ON event_publication USING hash (serialized_event);
CREATE INDEX event_publication_by_completion_date_idx ON event_publication (completion_date);

-- 2) Enquanto está sendo lido ou esperando conferência, o certificado ainda não tem os dados.
ALTER TABLE certificado ALTER COLUMN titulo DROP NOT NULL;
ALTER TABLE certificado ALTER COLUMN item_numero DROP NOT NULL;
ALTER TABLE certificado ALTER COLUMN horas_certificado DROP NOT NULL;

ALTER TABLE certificado DROP CONSTRAINT ck_certificado_status;
ALTER TABLE certificado ADD CONSTRAINT ck_certificado_status CHECK (status IN
    ('PROCESSANDO', 'AGUARDANDO_CONFIRMACAO', 'CONFIRMADO', 'AJUSTE_SOLICITADO', 'DEFERIDO', 'INDEFERIDO'));

-- Depois de conferido, os dados são obrigatórios.
ALTER TABLE certificado ADD CONSTRAINT ck_certificado_dados_conferidos CHECK (
    status IN ('PROCESSANDO', 'AGUARDANDO_CONFIRMACAO')
    OR (titulo IS NOT NULL AND item_numero IS NOT NULL AND horas_certificado IS NOT NULL));

-- 3) O que a leitura automática encontrou. Fica separado do certificado para comparar sugerido × confirmado.
CREATE TABLE extracao (
    id                   UUID PRIMARY KEY,
    certificado_id       UUID         NOT NULL,      -- referência opaca: o módulo de extração não conhece o certificado
    metodo               VARCHAR(20)  NOT NULL,
    versao_extrator      VARCHAR(20)  NOT NULL,
    texto_bruto          TEXT,                       -- contém dados pessoais: ver política de retenção no plano (§8)
    titulo               VARCHAR(300),
    instituicao          VARCHAR(200),
    data_inicio          DATE,
    data_fim             DATE,
    horas                NUMERIC(6, 2),
    nome_confere         BOOLEAN,
    trechos              TEXT[]       NOT NULL DEFAULT '{}',
    avisos               TEXT[]       NOT NULL DEFAULT '{}',
    duracao_ms           BIGINT       NOT NULL,
    criado_em            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_extracao_metodo CHECK (metodo IN ('TEXTO', 'OCR', 'SEM_TEXTO', 'FALHA'))
);

CREATE INDEX ix_extracao_certificado ON extracao (certificado_id, criado_em DESC);
