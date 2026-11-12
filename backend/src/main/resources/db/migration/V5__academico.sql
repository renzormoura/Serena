CREATE TABLE curso (
    id    UUID PRIMARY KEY,
    nome  VARCHAR(200) NOT NULL,
    sigla VARCHAR(20),
    ativo BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_curso_nome UNIQUE (nome)
);

-- A planilha atual é do "Curso de Engenharia". As habilitações (Civil, Elétrica...) entram quando a
-- coordenação confirmar a lista (decisão D5).
INSERT INTO curso (id, nome, sigla) VALUES ('00000000-0000-0000-0000-0000000000c1', 'Engenharia', 'ENG');

CREATE TABLE aluno (
    id                UUID PRIMARY KEY,
    usuario_id        UUID        NOT NULL REFERENCES usuario (id),
    matricula         VARCHAR(30) NOT NULL,
    curso_id          UUID        NOT NULL REFERENCES curso (id),
    regulamento_id    UUID        NOT NULL REFERENCES regulamento (id),
    ano_ingresso      INTEGER     NOT NULL,
    semestre_ingresso INTEGER     NOT NULL,
    criado_em         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_aluno_usuario UNIQUE (usuario_id),
    CONSTRAINT uk_aluno_matricula UNIQUE (matricula),
    CONSTRAINT ck_aluno_semestre CHECK (semestre_ingresso IN (1, 2)),
    CONSTRAINT ck_aluno_ano CHECK (ano_ingresso BETWEEN 1990 AND 2100)
);
