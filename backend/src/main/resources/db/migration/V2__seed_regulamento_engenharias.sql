-- Catálogo importado da planilha 'HORAS COMPLEMENTARES - Modelo Engs.xlsx' (aba INTEGRALIZAÇÃO), sem correções.
-- Itens 6, 7, 34, 37 e 52 têm pendências de revisão com a coordenação (ver docs/PLANO-DO-PROJETO.md §1.3).
INSERT INTO regulamento (id, nome, versao, vigente_desde, horas_exigidas, publicado)
VALUES ('00000000-0000-0000-0000-000000000001', 'Regulamento de Atividades Complementares — Engenharias', 1, NULL, NULL, TRUE);

INSERT INTO macroatividade (id, regulamento_id, codigo, ordem, nome, limite_horas) VALUES (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'CIDADANIA', 1, 'Cidadania', NULL);
INSERT INTO macroatividade (id, regulamento_id, codigo, ordem, nome, limite_horas) VALUES (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'CIENTIFICO_ACADEMICO', 2, 'Científico-Acadêmico', NULL);
INSERT INTO macroatividade (id, regulamento_id, codigo, ordem, nome, limite_horas) VALUES (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'CAPACITACAO_EMPREENDEDORISMO', 3, 'Capacitação Profissional, Empreendedorismo e Inovação', NULL);
INSERT INTO macroatividade (id, regulamento_id, codigo, ordem, nome, limite_horas) VALUES (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'EXTERNAS', 4, 'Atividades Complementares Externas', NULL);

INSERT INTO item_atividade (id, regulamento_id, macroatividade_id, numero, nome, descricao, horas_por_ocorrencia, estrategia)
SELECT gen_random_uuid(), m.regulamento_id, m.id, v.numero, v.nome, v.descricao, v.horas, 'FIXO_POR_OCORRENCIA'
FROM (VALUES
    ('CIDADANIA', 1, 'Atividades Culturais e Esportivas na Universidade', 'Eventos culturais e esportivos na universidade', 10),
    ('CIDADANIA', 2, 'Atividades em instituições de Ensino', 'Utilização da expertise de cada curso para a prestação de serviços para professores, pais e alunos em escolas públicas e particulars.', 25),
    ('CIDADANIA', 3, 'Atividades Nacionais de Cidadania', 'Eventos com a temática de Cidadania oferecidos em âmbito nacional na modalidade à distância.', 5),
    ('CIDADANIA', 4, 'Evento de Cidadania e Direitos Humanos', 'Evento que deve, preferencialmente, envolver todos os cursos da unidade para oferecer atendimento gratuito de saúde, educação e cidadania à população local, além de atividades de lazer e esporte.', 20),
    ('CIDADANIA', 5, 'Prestação de Serviços à Comunidade', 'Utilização da expertise de cada curso para o fornecimento de serviços que contribuam para o desenvolvimento das comunidades locais.', 25),
    ('CIDADANIA', 6, 'Presidente do DCE', NULL, 25),
    ('CIDADANIA', 7, 'Presidente do Centro Acadêmico', NULL, 25),
    ('CIDADANIA', 8, 'Ações Sócios Ambientais', 'É uma série de passeios históricos por bairros da região. Uma “aula ao vivo”, onde se é mostrada uma cidade moldada pelo seu conjunto arquitetônico e seus bens imateriais (personagens, acontecimentos, cheiros e sabores). São trabalhar os aspectos históricos, literários, sociológicos.', 20),
    ('CIDADANIA', 9, 'Trote Solidário', 'Recepção dos calouros com foco em cidadania e solidariedade.', 10),
    ('CIENTIFICO_ACADEMICO', 10, 'CURSOS LIVRES', 'Cursos oferecidos pelo curso fora da grade curricular', 20),
    ('CIENTIFICO_ACADEMICO', 11, 'Disiplinas Extracurriculares', 'Aprovação em disciplinas fora da matriz curricular', 20),
    ('CIENTIFICO_ACADEMICO', 12, 'Enade Entrega da Prova', 'Realização e entrega da prova do ENADE', 15),
    ('CIENTIFICO_ACADEMICO', 13, 'Enade Simulados Presenciais', 'Participação dos simulados do ENADE oferecidos pela Instituição online ou presencialmente', 10),
    ('CIENTIFICO_ACADEMICO', 14, 'Eventos Acadêmicos Científicos', 'Eventos oferecidos pela Universidade de Vassouras com o objetivo de incentivar a prática acadêmico-científica', 5),
    ('CIENTIFICO_ACADEMICO', 15, 'Grupo de Estudos', 'Grupos de alunos que se reunem periodicamente para estudar determinada disciplina ou assunto sob supervisão de um professor responsável', 5),
    ('CIENTIFICO_ACADEMICO', 16, 'Iniciação Científica', 'Programas de iniciação científica nas unidades com participação de alunos para prática acadêmica', 50),
    ('CIENTIFICO_ACADEMICO', 17, 'Intercâmbio', 'Realizaram Intercâmbio em convênio da Universidade de Vassouras.', 50),
    ('CIENTIFICO_ACADEMICO', 18, 'Monitoria', 'Alunos monitores de alguma disciplina ou projeto, dentro da política da Universidade de Vassouras.', 50),
    ('CIENTIFICO_ACADEMICO', 19, 'Nivelamentos', 'Participação nos nivelamentos oferecidos pela Universidade de Vassouras.', 10),
    ('CIENTIFICO_ACADEMICO', 20, 'Núcleos de Práticas Acadêmicas', 'Espaços para a prática dos conteúdos desenvolvidos em sala de aula (Não se confunde com estágio obrigatório) através da realização de atividades de cunho social ou não', 50),
    ('CIENTIFICO_ACADEMICO', 21, 'Oficinas Acadêmicas', 'Participação em oficinas acadêmicas em geral', 10),
    ('CIENTIFICO_ACADEMICO', 22, 'Palestras Presenciais', 'Participação em palestras oferecidas pela Universidade de Vassouras.', 5),
    ('CIENTIFICO_ACADEMICO', 23, 'Projeto Boas- Vindas', 'Participação nas ações de Boas-Vindas', 10),
    ('CIENTIFICO_ACADEMICO', 24, 'Projeto de Extensão Universitária', 'Participação em projetos de extensão universitária.', 50),
    ('CIENTIFICO_ACADEMICO', 25, 'Semana Acadêmica, Jornada Severino Sombra e ENIC', 'Evento específico direcionado a uma área ou a um curso (semana do curso).', 5),
    ('CIENTIFICO_ACADEMICO', 26, 'Trabalho Extraclasse', 'Trata-se dos trabalhos realizados pelos alunos fora do período de aula e que serão avaliados pelos professores de determinada disciplina sendo atribuídas aos alunos que obtiverem conceito satisfatório horas ao invés de pontos nas avaliações da disciplina', 5),
    ('CAPACITACAO_EMPREENDEDORISMO', 27, 'Atividades de Empreendedorismo e Inovação', 'Atividades oferecidas pela Universidade de Vassouras com o objetivo de ampliar a visão do aluno a respeito dos campos de atuação profissional', 25),
    ('CAPACITACAO_EMPREENDEDORISMO', 28, 'Atividades de Capacitação Profissional', 'Atividades oferecidas pela Universidade de Vassouras com o objetivo de preparar o aluno para processos seletivos e para mercado de trabalho', 10),
    ('CAPACITACAO_EMPREENDEDORISMO', 29, 'Atividades Nacionais de Empreendedorismo e Inovação', 'Eventos com essa temática oferecidos em âmbito nacional na modalidade à distância', 5),
    ('CAPACITACAO_EMPREENDEDORISMO', 30, 'Atividades Nacionais de Capacitação Profissional', 'Eventos com essa temática oferecidos em âmbito nacional na modalidade à distância', 5),
    ('CAPACITACAO_EMPREENDEDORISMO', 31, 'Auxílio na Organização de Eventos', 'Participação do aluno como organizador de eventos da unidade sob supervisão de um professor Responsável', 10),
    ('CAPACITACAO_EMPREENDEDORISMO', 32, 'Desafio Inovação', 'Desafio dentro do Ambiente Virtual de Aprendizagem com o tema Inovação para a participação dos alunos', 10),
    ('CAPACITACAO_EMPREENDEDORISMO', 33, 'Encontro com Egressos', 'Encontro de ex-alunos com atividades para alunos da unidade e de escolas públicas. Essa atividade visa valorizar o egresso, fomentar o networking, estimular o alunado e apresentar para possíveis alunos o bom desempenho profissional de nossos egressos', 10),
    ('CAPACITACAO_EMPREENDEDORISMO', 34, 'NTO DE EMPREENDEDORISMO E INOVAÇÃO', 'Evento sobre a temática oferecido pela Universidade de Vassouras com o objetivo de ampliar a visão do aluno a respeito dos campos de atuação profissional', 5),
    ('CAPACITACAO_EMPREENDEDORISMO', 35, 'OLIMPÍADAS E CONCURSOS', 'Competições locais ou nacionais para incentivar a cultura do estudo e desenvolver competências e habilidades.', 20),
    ('CAPACITACAO_EMPREENDEDORISMO', 36, 'Representante de Turma', 'Atuação como representante de turma', 20),
    ('CAPACITACAO_EMPREENDEDORISMO', 37, 'Consu/ Consepe', 'Atuação como representante de turma', 20),
    ('CAPACITACAO_EMPREENDEDORISMO', 38, 'Viagens de Excursões Acadêmicas', 'Viagens organizadas e oferecidas pelas unidades que proporcionem o encontro do mundo acadêmico com o profissional', 25),
    ('CAPACITACAO_EMPREENDEDORISMO', 39, 'Visitas técnicas', 'Visitas organizadas e oferecidas pela Universidade de Vassouras que proporcionem o encontro do mundo acadêmico com o profissional', 10),
    ('EXTERNAS', 40, 'Eventos Acadêmico-Científicos', 'Participação de congressos, seminários, simpósios, fóruns, mesas redondas, conferências e outros eventos do tipo com abrangência Nacional', 10),
    ('EXTERNAS', 41, 'Certificações na Área do Curso', 'Certificações na área do curso', 20),
    ('EXTERNAS', 42, 'Trabalho Publicado/Premiado', 'Trabalhos publicados/premiados em eventos científicos e trabalhos ganhadores de concursos', 20),
    ('EXTERNAS', 43, 'Apresentação de Trabalho em Evento Acadêmico-Científico', 'Apresentação de trabalho acadêmico em eventos científicos', 20),
    ('EXTERNAS', 44, 'Eventos Culturais Externos', 'Participação de eventos culturais diversos tais como: bienais, peças teatrais, cinema, museus, feiras culturais e saraus (ingresso)', 20),
    ('EXTERNAS', 45, 'Vivência Profissional na Área do Curso', 'Atividade profissional remunerada ou não, incluindo estágio e emprego (exceto estágio obrigatório)', 30),
    ('EXTERNAS', 46, 'Cursos Online e Presenciais', 'Cursos online e presenciais desvinculados à instituição, incluindo cursos profissionalizantes e de línguas', 30),
    ('EXTERNAS', 47, 'Trabalho Voluntário', 'Trabalho voluntário em instituições, processo eleitoral', 40),
    ('EXTERNAS', 48, 'Palestras Externas', 'Palestras fora da instituição mediante apresentação de certificado de participação', 5),
    ('EXTERNAS', 49, 'Competições Universitárias Nacionais e Internacionais', 'Participação em competições universitárias', 25),
    ('EXTERNAS', 50, 'Intercâmbio ou Visita Técnica Fora do Estado do Rio de Janeiro', 'Realização de intercâmbio sem convênio da Universidade de Vassouras.', 100),
    ('EXTERNAS', 51, 'Eventos Étnicos Raciais', 'Participação em eventos relativo à educação étnico-raciais', 20),
    ('EXTERNAS', 52, 'Projeto Rondon', 'Realização de intercâmbio sem convênio da Universidade de Vassouras', 100)
) AS v(macro, numero, nome, descricao, horas)
JOIN macroatividade m ON m.codigo = v.macro AND m.regulamento_id = '00000000-0000-0000-0000-000000000001';
