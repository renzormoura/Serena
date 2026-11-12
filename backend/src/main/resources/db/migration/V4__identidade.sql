CREATE TABLE usuario (
    id              UUID PRIMARY KEY,
    email           VARCHAR(254) NOT NULL,
    nome            VARCHAR(200) NOT NULL,
    senha_hash      VARCHAR(200) NOT NULL,
    papel           VARCHAR(20)  NOT NULL,
    ativo           BOOLEAN      NOT NULL DEFAULT TRUE,
    ultimo_login_em TIMESTAMPTZ,
    criado_em       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_usuario_papel CHECK (papel IN ('ALUNO', 'COORDENADOR', 'ADMIN')),
    CONSTRAINT ck_usuario_email_minusculo CHECK (email = lower(email))
);

CREATE UNIQUE INDEX uk_usuario_email ON usuario (email);
