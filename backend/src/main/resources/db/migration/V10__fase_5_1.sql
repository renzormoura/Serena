-- Fase 5.1: ajustes pedidos em 11/11/2026 (PLANO-DO-PROJETO.md §18, "Fase 5.1").

-- 1) As quatro engenharias. O curso genérico "Engenharia" sai do cadastro; quem estava nele vai para Civil
--    (não havia alunos reais ainda, só contas de teste).
INSERT INTO curso (id, nome, sigla) VALUES
    ('00000000-0000-0000-0000-0000000000c2', 'Engenharia Civil', 'ECIV'),
    ('00000000-0000-0000-0000-0000000000c3', 'Engenharia Elétrica', 'EELE'),
    ('00000000-0000-0000-0000-0000000000c4', 'Engenharia Química', 'EQUI'),
    ('00000000-0000-0000-0000-0000000000c5', 'Engenharia de Software', 'ESOF');

UPDATE aluno SET curso_id = '00000000-0000-0000-0000-0000000000c2'
WHERE curso_id = '00000000-0000-0000-0000-0000000000c1';

-- Quem coordenava o curso genérico passa a coordenar as quatro engenharias.
INSERT INTO coordenacao (usuario_id, curso_id)
SELECT co.usuario_id, novo.id
FROM coordenacao co
CROSS JOIN curso novo
WHERE co.curso_id = '00000000-0000-0000-0000-0000000000c1'
  AND novo.id IN ('00000000-0000-0000-0000-0000000000c2', '00000000-0000-0000-0000-0000000000c3',
                  '00000000-0000-0000-0000-0000000000c4', '00000000-0000-0000-0000-0000000000c5')
ON CONFLICT DO NOTHING;
DELETE FROM coordenacao WHERE curso_id = '00000000-0000-0000-0000-0000000000c1';
UPDATE curso SET ativo = FALSE WHERE id = '00000000-0000-0000-0000-0000000000c1';

-- 2) O cadastro não pede mais o ingresso. A coordenação pode informá-lo depois (usado no período do aluno).
ALTER TABLE aluno ALTER COLUMN ano_ingresso DROP NOT NULL;
ALTER TABLE aluno ALTER COLUMN semestre_ingresso DROP NOT NULL;

-- 3) Observação do aluno para quem analisa (opcional, até 250 caracteres).
ALTER TABLE certificado ADD COLUMN observacao_aluno VARCHAR(250);

-- 4) Metadados do PDF, usados pela triagem automática (alerta "PDF editado").
ALTER TABLE extracao ADD COLUMN pdf_produtor     VARCHAR(300);
ALTER TABLE extracao ADD COLUMN pdf_criador      VARCHAR(300);
ALTER TABLE extracao ADD COLUMN pdf_criado_em    TIMESTAMPTZ;
ALTER TABLE extracao ADD COLUMN pdf_modificado_em TIMESTAMPTZ;

-- Para achar o mesmo arquivo enviado por contas diferentes.
CREATE INDEX ix_arquivo_sha256_global ON arquivo (sha256);

-- 5) Avisos no site para o aluno (resultado da análise).
CREATE TABLE aviso (
    id         UUID PRIMARY KEY,
    usuario_id UUID          NOT NULL REFERENCES usuario (id),
    titulo     VARCHAR(200)  NOT NULL,
    mensagem   VARCHAR(1500) NOT NULL,
    link       VARCHAR(300),
    lido_em    TIMESTAMPTZ,
    criado_em  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX ix_aviso_usuario ON aviso (usuario_id, criado_em DESC);
