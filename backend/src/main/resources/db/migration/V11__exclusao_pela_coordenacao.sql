-- A coordenação pode excluir um certificado de um aluno (com motivo). Fica no histórico como EXCLUIDO.
ALTER TABLE avaliacao DROP CONSTRAINT ck_avaliacao_decisao;
ALTER TABLE avaliacao ADD CONSTRAINT ck_avaliacao_decisao
    CHECK (decisao IN ('DEFERIDO', 'INDEFERIDO', 'AJUSTE_SOLICITADO', 'REABERTO', 'EXCLUIDO'));
