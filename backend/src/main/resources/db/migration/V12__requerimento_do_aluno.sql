-- O aluno também gera o requerimento. Nesse caso ainda não há deferimento: data e coordenador ficam em branco
-- no documento, para a coordenação preencher e assinar.
ALTER TABLE requerimento ALTER COLUMN coordenador_id DROP NOT NULL;
ALTER TABLE requerimento ALTER COLUMN coordenador_nome DROP NOT NULL;
ALTER TABLE requerimento ALTER COLUMN data_deferimento DROP NOT NULL;
ALTER TABLE requerimento ADD COLUMN gerado_pelo_aluno BOOLEAN NOT NULL DEFAULT FALSE;
