-- Fase F3: sugestão do item do regulamento por regras (sem IA — decisão D7, 11/11/2026).

-- De onde a atividade costuma vir: ajuda a separar itens parecidos (ex.: palestra interna 22 × externa 48).
ALTER TABLE item_atividade ADD COLUMN origem_esperada VARCHAR(10) NOT NULL DEFAULT 'QUALQUER';
ALTER TABLE item_atividade ADD CONSTRAINT ck_item_origem CHECK (origem_esperada IN ('INTERNA', 'EXTERNA', 'QUALQUER'));

-- O que o sistema sugeriu, para mostrar alternativas e medir o acerto (sugerido × confirmado pelo aluno).
ALTER TABLE certificado ADD COLUMN item_sugerido INTEGER;
ALTER TABLE certificado ADD COLUMN confianca_sugestao VARCHAR(10);
ALTER TABLE certificado ADD COLUMN alternativas_sugeridas INTEGER[] NOT NULL DEFAULT '{}';
ALTER TABLE certificado ADD COLUMN motivo_sugestao VARCHAR(500);
ALTER TABLE certificado ADD CONSTRAINT ck_certificado_confianca CHECK (confianca_sugestao IS NULL OR confianca_sugestao IN ('ALTA', 'MEDIA', 'BAIXA'));
CREATE INDEX ix_certificado_sugestao ON certificado (item_sugerido) WHERE item_sugerido IS NOT NULL AND excluido_em IS NULL;

-- Palavras-chave iniciais (sem acento e sem 'de/da/do'), tiradas das descrições do regulamento.
-- Prefixo '!' = palavra forte (peso dobrado): o papel do aluno (monitor, organizador...) decide mais que o nome do evento.
-- A administração ajusta depois, guiada pela taxa de acerto.
UPDATE item_atividade i SET palavras_chave = v.palavras, origem_esperada = v.origem
FROM (VALUES
    (1, 'INTERNA', ARRAY['evento cultural', 'evento esportivo', 'torneio', 'campeonato', 'gincana', 'festival', 'jogos universitarios']::TEXT[]),
    (2, 'QUALQUER', ARRAY['escola publica', 'escola municipal', 'escola estadual', 'colegio', 'ensino medio', 'ensino fundamental', 'escolas']::TEXT[]),
    (3, 'EXTERNA', ARRAY['cidadania', 'direitos humanos', 'a distancia', 'ead']::TEXT[]),
    (4, 'INTERNA', ARRAY['cidadania', 'direitos humanos', 'atendimento gratuito', 'acao social', 'acao cidada']::TEXT[]),
    (5, 'QUALQUER', ARRAY['prestacao servicos', 'servico comunidade', 'servicos comunidade', 'comunidade local', 'atendimento comunidade']::TEXT[]),
    (6, 'INTERNA', ARRAY['!presidente dce', 'diretorio central estudantes', 'dce']::TEXT[]),
    (7, 'INTERNA', ARRAY['!presidente centro academico', 'centro academico', 'diretorio academico']::TEXT[]),
    (8, 'QUALQUER', ARRAY['socioambiental', 'socio ambiental', 'meio ambiente', 'ambiental', 'reflorestamento', 'coleta seletiva', 'passeio historico', 'patrimonio historico']::TEXT[]),
    (9, 'INTERNA', ARRAY['trote solidario', 'doacao sangue', 'arrecadacao alimentos', 'recepcao calouros']::TEXT[]),
    (10, 'INTERNA', ARRAY['curso livre', 'curso extensao', 'minicurso']::TEXT[]),
    (11, 'INTERNA', ARRAY['disciplina extracurricular', 'disciplina optativa', 'disciplina eletiva', 'aprovacao disciplina']::TEXT[]),
    (12, 'INTERNA', ARRAY['enade', 'prova enade', 'exame nacional desempenho estudantes']::TEXT[]),
    (13, 'INTERNA', ARRAY['simulado enade', 'simulado']::TEXT[]),
    (14, 'INTERNA', ARRAY['evento academico', 'evento cientifico', 'jornada cientifica', 'mostra cientifica']::TEXT[]),
    (15, 'INTERNA', ARRAY['grupo estudos', 'grupo estudo']::TEXT[]),
    (16, 'QUALQUER', ARRAY['iniciacao cientifica', 'pibic', 'pibiti', '!bolsista iniciacao']::TEXT[]),
    (17, 'INTERNA', ARRAY['intercambio', 'mobilidade academica', 'convenio']::TEXT[]),
    (18, 'INTERNA', ARRAY['!monitoria', '!monitor disciplina', '!monitora disciplina', '!monitor', '!monitora']::TEXT[]),
    (19, 'INTERNA', ARRAY['nivelamento', 'curso nivelamento']::TEXT[]),
    (20, 'INTERNA', ARRAY['nucleo pratica', 'nucleo praticas', 'pratica academica', 'escritorio modelo', 'empresa junior']::TEXT[]),
    (21, 'QUALQUER', ARRAY['oficina', 'workshop']::TEXT[]),
    (22, 'INTERNA', ARRAY['palestra']::TEXT[]),
    (23, 'INTERNA', ARRAY['boas vindas', 'semana recepcao', 'acolhimento']::TEXT[]),
    (24, 'INTERNA', ARRAY['projeto extensao', 'extensao universitaria', 'acao extensionista', 'mostra extensao', 'extensionista']::TEXT[]),
    (25, 'INTERNA', ARRAY['semana academica', 'semana engenharia', 'semana engenharias', 'jornada severino sombra', 'enic', 'encontro iniciacao cientifica']::TEXT[]),
    (26, 'INTERNA', ARRAY['trabalho extraclasse', 'atividade extraclasse']::TEXT[]),
    (27, 'INTERNA', ARRAY['empreendedorismo', 'inovacao', 'startup', 'hackathon', 'pitch', 'modelo negocios']::TEXT[]),
    (28, 'INTERNA', ARRAY['capacitacao profissional', 'mercado trabalho', 'processo seletivo', 'entrevista emprego', 'carreira', 'empregabilidade']::TEXT[]),
    (29, 'EXTERNA', ARRAY['empreendedorismo', 'inovacao', 'startup', 'a distancia', 'ead']::TEXT[]),
    (30, 'EXTERNA', ARRAY['capacitacao profissional', 'mercado trabalho', 'carreira', 'empregabilidade', 'a distancia', 'ead']::TEXT[]),
    (31, 'INTERNA', ARRAY['!comissao organizadora', 'organizacao evento', '!organizador', '!organizadora', 'apoio organizacao']::TEXT[]),
    (32, 'INTERNA', ARRAY['desafio inovacao', 'ambiente virtual aprendizagem']::TEXT[]),
    (33, 'INTERNA', ARRAY['egressos', 'encontro egressos', 'ex alunos']::TEXT[]),
    (34, 'INTERNA', ARRAY['evento empreendedorismo', 'semana empreendedorismo', 'feira inovacao']::TEXT[]),
    (35, 'QUALQUER', ARRAY['olimpiada', 'olimpiadas', 'concurso', 'maratona programacao']::TEXT[]),
    (36, 'INTERNA', ARRAY['!representante turma', '!lider turma']::TEXT[]),
    (37, 'INTERNA', ARRAY['consu', 'consepe', 'conselho universitario', 'conselho ensino pesquisa extensao']::TEXT[]),
    (38, 'INTERNA', ARRAY['viagem academica', 'excursao', 'viagem tecnica', 'viagem estudos']::TEXT[]),
    (39, 'INTERNA', ARRAY['visita tecnica', 'visita empresa', 'visita guiada']::TEXT[]),
    (40, 'EXTERNA', ARRAY['congresso', 'seminario', 'simposio', 'forum', 'mesa redonda', 'conferencia', 'encontro nacional']::TEXT[]),
    (41, 'QUALQUER', ARRAY['certificacao', 'exame certificacao', 'certified', 'ccna', 'pmp', 'nr 10', 'nr 35']::TEXT[]),
    (42, 'QUALQUER', ARRAY['trabalho publicado', 'publicacao', 'artigo publicado', 'premiado', 'premio', 'primeiro lugar', 'mencao honrosa', 'anais']::TEXT[]),
    (43, 'QUALQUER', ARRAY['!apresentou trabalho', '!apresentacao trabalho', 'apresentacao oral', 'poster', 'comunicacao oral', 'autor trabalho']::TEXT[]),
    (44, 'EXTERNA', ARRAY['evento cultural', 'bienal', 'teatro', 'peca teatral', 'cinema', 'museu', 'feira cultural', 'sarau', 'exposicao', 'show']::TEXT[]),
    (45, 'EXTERNA', ARRAY['!estagio', '!estagiario', 'vinculo empregaticio', 'atividade profissional', 'carteira trabalho']::TEXT[]),
    (46, 'EXTERNA', ARRAY['curso', 'curso online', 'curso presencial', 'curso profissionalizante', 'curso idiomas', 'ingles', 'espanhol', '!formacao', 'treinamento', 'bootcamp', 'trilha']::TEXT[]),
    (47, 'QUALQUER', ARRAY['!voluntario', '!voluntaria', 'trabalho voluntario', 'voluntariado', '!mesario', '!mesaria', 'processo eleitoral', 'justica eleitoral']::TEXT[]),
    (48, 'EXTERNA', ARRAY['palestra', 'webinar', 'live']::TEXT[]),
    (49, 'QUALQUER', ARRAY['competicao universitaria', 'baja', 'aerodesign', 'formula sae', 'robocup', 'competicao robotica']::TEXT[]),
    (50, 'QUALQUER', ARRAY['fora estado', 'outro estado', 'fora estado rio janeiro', 'intercambio sem convenio']::TEXT[]),
    (51, 'QUALQUER', ARRAY['etnico racial', 'etnico raciais', 'relacoes etnico', 'consciencia negra', 'racismo', 'antirracismo', 'afro brasileira', 'indigena']::TEXT[]),
    (52, 'QUALQUER', ARRAY['projeto rondon', 'rondon']::TEXT[])
) AS v(numero, origem, palavras)
WHERE i.numero = v.numero AND i.regulamento_id = '00000000-0000-0000-0000-000000000001';
