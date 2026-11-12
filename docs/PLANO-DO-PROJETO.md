# Sereno — Sistema de Horas Complementares (Engenharias · Universidade de Vassouras)

> Plano de projeto, arquitetura e roadmap. Versão 0.1 — 11/11/2026.
> Status: **rascunho para decisão**. Nada foi implementado ainda.

---

## 0. Resumo executivo

Substituir a planilha `HORAS COMPLEMENTARES - Modelo Engs.xlsx` por um sistema web onde:

1. o **aluno** cria conta, envia o PDF de cada certificado;
2. o sistema **extrai** o texto do PDF, **identifica a carga horária** e **sugere o item** do regulamento (1–52) em que a atividade se enquadra;
3. o aluno **confirma ou corrige** horas e item antes de salvar;
4. o **coordenador** defere/indefere (se o fluxo de deferimento for adotado — ver §2);
5. contadores mostram **horas integralizadas, pendentes e faltantes**;
6. dashboards para aluno e coordenação; exportação do requerimento no formato oficial atual.

**Stack recomendada:** Java 25 (LTS) + Spring Boot 4.x como monólito modular (Spring Modulith), PostgreSQL, armazenamento S3‑compatível para os PDFs, frontend React + TypeScript (Vite). A escolha de Java/Spring proposta é endossada — justificativa em §6.

**Primeira entrega útil (MVP, fase 1):** cadastro + catálogo + lançamento manual de certificados + contador. Já substitui a planilha *sem* depender de IA. A automação do PDF entra nas fases seguintes.

---

## 1. Análise da planilha atual

### 1.1 Estrutura

| Aba | Conteúdo |
|---|---|
| **INTEGRALIZAÇÃO** | O requerimento oficial: cabeçalho (Pró‑Reitoria de Ciências Tecnológicas e Exatas / Curso de Engenharia), dados do aluno (nome, matrícula, RG, CPF), texto do requerimento ao coordenador, e a tabela com os **52 itens** agrupados em **4 macroatividades**. Colunas: ITEM, MACROATIVIDADE, DESCRIÇÃO, HORAS ACC, QTD, HORAS INTEGRALIZADAS. Total em `G67`. |
| **DESCRITIVO** | Lançamentos do aluno: Nº, Item (código 1–52), breve descritivo, local, horas do certificado. Ao lado, uma **tabela dinâmica** que conta certificados por item. |

Macroatividades e faixas de itens:

| Macroatividade | Itens |
|---|---|
| Cidadania | 1–9 |
| Científico‑Acadêmico | 10–26 |
| Capacitação Profissional, Empreendedorismo e Inovação | 27–39 |
| Atividades Complementares Externas | 40–52 |

O catálogo completo (seed do banco) está no **Apêndice A**.

### 1.2 Regra de cálculo real (importante)

```
QTD(item)                 = nº de certificados lançados no DESCRITIVO com aquele item   (tabela dinâmica + VLOOKUP)
HORAS INTEGRALIZADAS(item) = HORAS ACC(item) × QTD(item)
TOTAL                      = Σ HORAS INTEGRALIZADAS
```

**A carga horária do certificado (coluna E do DESCRITIVO) não entra no cálculo.** Cada certificado vale um valor fixo definido pelo item. Evidências no próprio exemplo:

| Certificado | Horas no certificado | Item | Horas integralizadas |
|---|---|---|---|
| Formação Gerente Ágil (Alura) | 70 | 10 – Cursos Livres | **20** |
| IV Mostra de Extensão 2023 | 3 | 24 – Projeto de Extensão | **50** |
| Curso de extensão SQL | 20 | 19 – Nivelamentos | **10** |

Total do exemplo: 13 certificados → **430 h**. (5 certificados de 3 h no item 24 renderam 250 h.)

Consequência para o sistema: o motor de cálculo precisa suportar **mais de uma estratégia** (ver §9), porque essa regra pode ser (a) a regra oficial, (b) uma simplificação da planilha, ou (c) mudar em um novo regulamento. **Precisa ser confirmada com a coordenação** antes da fase 1.

### 1.3 Problemas encontrados na planilha (que o sistema resolve)

1. **Tabela dinâmica precisa de "Atualizar" manual** — se o aluno esquece, o total fica errado silenciosamente.
2. `Deferido em: =HOJE()` — a data de deferimento muda toda vez que o arquivo é aberto; não registra a data real.
3. **Não existe total exigido nem limites** por item/macroatividade — o "quanto falta" não é calculável hoje.
4. Classificação 100% manual e sujeita a erro (ex.: "curso ministrado na semana das engenharias" classificado como item 4 – *Evento de Cidadania e Direitos Humanos*; "curso de extensão SQL" como 19 – *Nivelamentos*). Nenhum registro de quem conferiu.
5. Dados inconsistentes no catálogo:
   - item 34 com nome truncado: `NTO DE EMPREENDEDORISMO E INOVAÇÃO` (provável "EVENTO DE …");
   - item 37 (Consu/Consepe) com a descrição copiada do item 36;
   - item 52 (Projeto Rondon) com a descrição copiada do item 50;
   - itens 6 e 7 sem descrição; typos ("Disiplinas", "particulars", "PROFISIONAL").
6. Sem detecção de **certificado duplicado** (mesmo PDF lançado duas vezes).
7. RG e CPF em arquivo solto, sem controle de acesso (LGPD).
8. Nenhuma visão consolidada da turma/curso para a coordenação.

---

## 2. Decisões em aberto (responder antes de codar)

| # | Pergunta | Impacto | Default proposto se não houver resposta |
|---|---|---|---|
| D1 | **Quantas horas complementares são exigidas?** Varia por engenharia/matriz curricular? | Contador "faltam" | Parâmetro por regulamento, configurável pelo admin |
| D2 | **Existem limites** por item ou macroatividade (ex.: máx. X h em Cidadania, máx. N monitorias)? | Motor de regras | Suportar limites opcionais desde o início |
| D3 | A regra **"valor fixo por certificado"** (§1.2) é a oficial? Ou deveria ser `min(horas do certificado, teto do item)`? | Cálculo | Estratégia `FIXO_POR_OCORRENCIA` como default, configurável por item |
| D4 | Há **deferimento pelo coordenador**, ou o sistema é só de autocontrole do aluno? | Fluxo, perfis, escopo | Implementar fluxo com deferimento (é o que o requerimento oficial pressupõe) |
| D5 | Quais engenharias usam o sistema? O regulamento é o mesmo para todas? | Modelo de dados | Multi‑curso desde o início; um regulamento compartilhado |
| D6 | Login: conta própria (e‑mail/senha) ou **SSO institucional** (Google Workspace / Microsoft 365)? | Auth | Conta própria + OIDC plugável depois |
| D7 | Pode enviar o texto do certificado a um **provedor de IA externo**? (LGPD, termo de consentimento) | Classificação | **Decidido em 11/11/2026: não.** Sem IA; classificação só por regras + conferência do aluno |
| D8 | **Onde hospedar**: servidor da universidade ou nuvem? Quem paga? | Infra | Docker em VPS/nuvem; nada preso a um provedor |
| D9 | É realmente necessário armazenar **RG e CPF**? | LGPD | Guardar só matrícula; CPF opcional e criptografado |
| D10 | Há regra de **validade temporal** (só atividades durante o curso, prazo de entrega)? | Validação | Campo de data obrigatório + alerta, sem bloqueio |

---

## 3. Requisitos

### 3.1 Funcionais

**Contas e perfis**
- RF01 Cadastro do aluno (nome, e‑mail institucional, matrícula, curso/habilitação, ano de ingresso), confirmação por e‑mail.
- RF02 Login, logout, recuperação de senha; (opcional) login SSO.
- RF03 Perfis: `ALUNO`, `COORDENADOR`, `ADMIN` (ver §4).

**Catálogo / regulamento**
- RF04 CRUD de regulamento (versão, vigência, horas exigidas), macroatividades e itens (nome, descrição, horas por ocorrência, limites, palavras‑chave para classificação).
- RF05 Cada aluno fica vinculado a uma versão do regulamento (a do seu ingresso); mudar regulamento não altera retroativamente quem já está em outra versão.

**Certificados**
- RF06 Upload de PDF (também JPG/PNG de certificado fotografado), até 10 MB, um ou vários de uma vez.
- RF07 Extração automática: título da atividade, instituição, data(s), **carga horária**, nome do participante.
- RF08 Sugestão automática do **item (1–52)** com nível de confiança e justificativa curta.
- RF09 **Tela de confirmação**: PDF ao lado do formulário pré‑preenchido; pergunta explícita "As horas e o item estão corretos?"; aluno pode editar todos os campos antes de salvar. Mostra em tempo real quantas horas aquele certificado vai integralizar.
- RF10 Edição posterior enquanto o certificado não foi deferido; após deferido, só com reabertura pelo coordenador.
- RF11 Detecção de duplicidade (hash do arquivo + título/data semelhantes) com aviso.
- RF12 Aviso quando o nome do aluno não aparece no texto do certificado.
- RF13 Exclusão (lógica) de certificado não deferido.

**Contadores**
- RF14 Horas **integralizadas** (deferidas), **pendentes** (confirmadas e aguardando análise), **faltantes** (`max(0, exigidas − integralizadas)`) e projeção (`integralizadas + pendentes`).
- RF15 Quebra por macroatividade e por item, mostrando limites atingidos e horas descartadas por teto.

**Coordenação**
- RF16 Fila de análise com filtros (curso, período, item, status, confiança baixa primeiro).
- RF17 Deferir / indeferir / solicitar ajuste, com justificativa; ajuste de item ou horas pelo coordenador (registrado em auditoria).
- RF18 Deferimento em lote.
- RF19 Gerar o **requerimento oficial** (PDF e XLSX no layout da aba INTEGRALIZAÇÃO) com data real de deferimento e nome do coordenador.

**Dashboards e relatórios** (§13)
- RF20 Dashboard do aluno.
- RF21 Dashboard da coordenação.
- RF22 Exportação CSV/XLSX.

**Migração**
- RF23 Importador da planilha atual (aba DESCRITIVO) para alunos que já têm horas lançadas.

### 3.2 Não funcionais

| Código | Requisito |
|---|---|
| RNF01 | Manutenibilidade: código modular com fronteiras verificadas em teste (Spring Modulith/ArchUnit); contrato da API em OpenAPI; cliente TS gerado. |
| RNF02 | Segurança: OWASP ASVS nível 2 como referência; senhas com Argon2/bcrypt; sessões HttpOnly + CSRF; rate‑limit em login e upload. |
| RNF03 | LGPD: minimização, criptografia de campos sensíveis, termo de uso/consentimento, exportação e exclusão dos dados do titular, logs de acesso. |
| RNF04 | Desempenho: telas < 300 ms p95 na API; processamento de um PDF < 10 s p95 (assíncrono). |
| RNF05 | Disponibilidade alvo 99,5%; backup diário do banco + versionamento dos arquivos; restore testado. |
| RNF06 | Escala esperada: milhares de alunos, dezenas de milhares de certificados — um único nó resolve com folga. |
| RNF07 | Acessibilidade WCAG 2.1 AA; responsivo (aluno vai enviar certificado pelo celular). |
| RNF08 | Rastreabilidade: toda alteração de horas/item/status fica auditada (quem, quando, antes/depois). |
| RNF09 | Idioma pt‑BR; datas `dd/MM/yyyy`; fuso America/Sao_Paulo. |

---

## 4. Perfis e permissões

| Ação | ALUNO | COORDENADOR | ADMIN |
|---|---|---|---|
| Enviar/editar os próprios certificados (não deferidos) | ✅ | — | — |
| Ver os próprios contadores e dashboard | ✅ | — | — |
| Ver certificados de alunos do(s) seu(s) curso(s) | — | ✅ | ✅ |
| Deferir / indeferir / ajustar | — | ✅ | ✅ |
| Dashboard da coordenação | — | ✅ (seus cursos) | ✅ (todos) |
| Gerir regulamento e catálogo | — | — | ✅ |
| Gerir usuários e papéis | — | — | ✅ |

Autorização por **escopo de curso**: coordenador vinculado a N cursos só enxerga esses alunos (checado no serviço, não só no frontend).

---

## 5. Fluxos

### 5.1 Ciclo de vida do certificado

```
             upload
               │
               ▼
         ┌───────────┐  falha de extração   ┌──────────────────────┐
         │PROCESSANDO├─────────────────────►│AGUARDANDO_CONFIRMACAO│ (campos em branco)
         └─────┬─────┘                      └──────────┬───────────┘
               │ extraído + classificado               │
               └──────────────────────────────────────►│
                                                       │ aluno confirma/edita
                                                       ▼
                                                ┌────────────┐
                                    ┌──────────►│ CONFIRMADO │ (conta como "pendente")
                                    │           └─────┬──────┘
                                    │  aluno corrige  │ coordenador analisa
                         ┌──────────┴────────┐        ├──────────────┬────────────────┐
                         │AJUSTE_SOLICITADO  │◄───────┤              │                │
                         └───────────────────┘        ▼              ▼                │
                                                 ┌─────────┐   ┌───────────┐           │
                                                 │DEFERIDO │   │INDEFERIDO │           │
                                                 └────┬────┘   └───────────┘           │
                                                      │ reabrir (coordenador) ─────────┘
```

Se o fluxo de deferimento for dispensado (D4), `CONFIRMADO` passa a contar como integralizado e as transições do coordenador são desligadas por configuração.

A máquina de estados fica em uma classe de domínio única (`CertificadoStatus` + método `transicionar(evento, ator)`) com teste de todas as transições válidas e inválidas.

### 5.2 Upload → confirmação (visão do aluno)

1. Aluno arrasta 1..N PDFs. API responde `202 Accepted` com os IDs.
2. Para cada arquivo, em background: calcula SHA‑256 → checa duplicidade → grava no storage → extrai texto (PDFBox; se pouco texto, OCR) → extrai campos (regex/heurísticas) → classifica (regras; IA se habilitada) → calcula horas integralizadas pela estratégia do item.
3. O frontend acompanha o status (polling leve ou SSE) e abre a **tela de confirmação**:
   - esquerda: visualizador do PDF com os trechos usados destacados (ex.: "carga horária de 20 horas");
   - direita: título, instituição, data, horas do certificado, **item sugerido** (com top‑3 alternativas e a confiança), horas que serão integralizadas;
   - pergunta: **"As horas e a classificação estão corretas?"** → `Sim, salvar` / `Corrigir`.
4. Ao salvar, o sistema guarda **o que foi sugerido e o que foi confirmado** (base para medir a acurácia da extração/classificação e melhorá‑la).

---

## 6. Stack e justificativa

### 6.1 Backend: Java 25 LTS + Spring Boot 4.x — **recomendado**

Por que é uma boa escolha para *este* projeto e para a manutenção posterior:
- Tipagem forte + ecossistema maduro para exatamente o que precisamos: Spring Security (sessões, OIDC), Spring Data JPA, Flyway, Bean Validation, **Apache PDFBox** (extração de PDF), **Tess4J** (OCR), **Apache POI** (ler a planilha legada e gerar o XLSX oficial), OpenPDF/Flying Saucer (gerar o requerimento em PDF).
- Spring Modulith permite um **monólito modular** com fronteiras verificadas — o caminho de menor custo de operação com organização de microserviço.
- Mão de obra: Java/Spring é amplamente ensinado e conhecido; um sucessor (inclusive aluno/estagiário) consegue dar manutenção.
- Virtual threads (Java 21+) dão concorrência barata para o processamento de PDFs sem precisar de framework reativo.
- Suporte LTS longo (Java 25, Spring com releases previsíveis).

> No dia do bootstrap, fixar a última versão **estável** do Spring Boot 4.x e do Spring Modulith compatível (via start.spring.io). Se alguma lib essencial ainda não suportar Boot 4, usar a última 3.5.x — a arquitetura não muda.

Alternativas consideradas:

| Opção | Por que não como padrão |
|---|---|
| Kotlin + Spring | Ótima, mas reduz o pool de mantenedores; Java moderno (records, sealed, pattern matching) já é enxuto. |
| Node/NestJS | Ecossistema de PDF/OCR/XLSX mais fraco; tipagem menos rígida em runtime. |
| Python/Django | Excelente para IA/PDF, mas o próprio responsável prefere Java; regras de negócio e segurança ficam melhores no Spring. |
| .NET | Equivalente técnico; sem vantagem para o contexto. |
| Microsserviços | Complexidade operacional injustificável para o volume. |

### 6.2 Frontend: React 19 + TypeScript + Vite

- **TanStack Query** (cache/estado servidor), **React Router**, **React Hook Form + Zod** (formulários e validação), **shadcn/ui + Tailwind** (componentes acessíveis), **Recharts** ou **ECharts** (dashboards), **react‑pdf/pdf.js** (visualizador na tela de confirmação).
- Cliente da API **gerado do OpenAPI** (springdoc → `orval` ou `openapi-typescript`): mudou o backend, o TS quebra na compilação — fundamental para manutenção.
- Alternativa se quiserem *uma só linguagem*: Thymeleaf + HTMX. Mais simples de hospedar, mas a tela de confirmação com PDF lado a lado e os dashboards ficam mais trabalhosos. Recomendo React.

### 6.3 Dados e infraestrutura

| Componente | Escolha |
|---|---|
| Banco | **PostgreSQL 17+** (JSONB para dados de extração, `pg_trgm` para busca/duplicidade por similaridade) |
| Migrações | **Flyway** (versionadas no repositório) |
| Arquivos | **S3‑compatível** (MinIO em dev/self‑host; R2/S3 em nuvem). PDFs nunca no banco. |
| Sessão | Spring Session JDBC (sessões sobrevivem a restart/escala) |
| E‑mail | SMTP (institucional ou SES/Resend) via Spring Mail |
| Proxy | Caddy ou Nginx (TLS automático, serve o SPA, `/api` → Spring) |
| Empacotamento | Docker + Docker Compose; imagem do backend via Buildpacks/Jib |
| CI/CD | GitHub Actions: build, testes, lint, scan de dependências, imagem, deploy |
| Observabilidade | Spring Actuator + Micrometer → Prometheus/Grafana (ou Grafana Cloud); logs JSON; Sentry para erros front/back |

---

## 7. Arquitetura

### 7.1 Visão geral

```
 Navegador (React SPA)
        │  HTTPS, cookie de sessão HttpOnly, CSRF
        ▼
 ┌──────────────────── Caddy/Nginx ─────────────────────┐
 │  /        → arquivos estáticos do SPA                │
 │  /api/**  → backend                                  │
 └──────────────────────────┬───────────────────────────┘
                            ▼
 ┌───────────────── Spring Boot (monólito modular) ─────────────────┐
 │ identidade │ academico │ regulamento │ certificados │ extracao   │
 │ avaliacao  │ calculo   │ relatorios  │ auditoria    │ notificacao│
 └───────┬──────────────────────┬─────────────────────────┬─────────┘
         ▼                      ▼                         ▼
    PostgreSQL           Storage S3 (PDFs)        [opcional] API de IA
```

### 7.2 Módulos (pacotes de topo, fronteiras verificadas por Spring Modulith)

| Módulo | Responsabilidade | Expõe |
|---|---|---|
| `identidade` | usuários, senhas, papéis, sessão, recuperação de senha, OIDC | `UsuarioAtual`, eventos `UsuarioCadastrado` |
| `academico` | curso, aluno, vínculo coordenador↔curso | consultas de aluno/curso |
| `regulamento` | regulamento versionado, macroatividades, itens, limites | `CatalogoService` (somente leitura para os demais) |
| `certificados` | agregado Certificado, máquina de estados, upload, arquivos | eventos `CertificadoEnviado`, `CertificadoConfirmado` |
| `extracao` | pipeline PDF → texto → campos → classificação | escuta `CertificadoEnviado`, publica `ExtracaoConcluida` |
| `calculo` | motor de horas (estratégias, limites, totais) | `CalculoService.saldo(alunoId)` — puro e determinístico |
| `avaliacao` | fila do coordenador, deferimento, ajustes, requerimento | eventos `CertificadoDeferido` — **Implementado na F4 de outro jeito:** as decisões ficaram no módulo `certificados` (as transições mudam o agregado `Certificado`, e separar exigiria expor o agregado); o requerimento virou o módulo `requerimento`; os e-mails, o módulo `notificacao` (evento `CertificadoAvaliado`). |
| `relatorios` | dashboards, exportações, read models | consultas agregadas |
| `auditoria` | trilha de alterações | escuta eventos de todos |
| `notificacao` | e‑mails (status mudou, ajuste solicitado) | escuta eventos |

Regras:
- Módulos só se comunicam pela **API pública** (pacote raiz do módulo) ou por **eventos de domínio** (`ApplicationEventPublisher` + *Event Publication Registry* do Spring Modulith, que garante entrega mesmo se a app cair no meio do processamento).
- `calculo` não acessa banco por conta própria: recebe a lista de certificados + regulamento e devolve o saldo — 100% testável sem Spring.
- Dentro de cada módulo: `api/` (controllers + DTOs), `domain/` (entidades, regras), `infra/` (repositórios, clientes externos). Sem anotações JPA vazando para DTOs; mapeamento com MapStruct ou records manuais.

### 7.3 Estrutura de repositório (monorepo)

```
sereno/
├── backend/                 # Spring Boot (Maven ou Gradle Kotlin DSL)
│   ├── src/main/java/br/edu/univassouras/sereno/<modulo>/...
│   ├── src/main/resources/db/migration/   # Flyway V1__..., V2__...
│   └── src/test/...                       # unit, integração (Testcontainers), modulith
├── frontend/                # React + Vite + TS
├── docs/                    # este plano, ADRs (docs/adr/0001-...), diagramas
├── infra/                   # docker-compose.yml, Caddyfile, scripts de backup
├── samples/                 # PDFs de certificado ANONIMIZADOS para testes de extração
└── .github/workflows/
```

Decisões arquiteturais registradas como **ADRs** curtos (contexto → decisão → consequências).

---

## 8. Modelo de dados (PostgreSQL)

Convenções: PK `uuid` (v7, ordenável), `criado_em`/`atualizado_em` `timestamptz`, `versao` para *optimistic locking*, exclusão lógica onde fizer sentido.

```
usuario            (id, email UNIQUE, senha_hash NULL, nome, papel, ativo, email_verificado,
                    provedor_oidc NULL, sujeito_oidc NULL, ultimo_login_em, criado_em, ...)

curso              (id, nome, sigla, ativo)                         -- Eng. Civil, Elétrica, ...
coordenacao        (usuario_id, curso_id)  PK composta

regulamento        (id, nome, versao, vigente_desde, vigente_ate NULL,
                    horas_exigidas, publicado BOOLEAN)
regulamento_curso  (regulamento_id, curso_id)
macroatividade     (id, regulamento_id, ordem, nome, limite_horas NULL)
item_atividade     (id, regulamento_id, macroatividade_id, numero,    -- 1..52
                    nome, descricao, horas_por_ocorrencia,
                    estrategia,           -- FIXO_POR_OCORRENCIA | HORAS_CERTIFICADO | HORAS_CERTIFICADO_COM_TETO
                    teto_por_ocorrencia NULL, limite_ocorrencias NULL, limite_horas NULL,
                    palavras_chave TEXT[], exemplos TEXT[], ativo,
                    UNIQUE(regulamento_id, numero))

aluno              (id, usuario_id UNIQUE, matricula UNIQUE, curso_id, regulamento_id,
                    ano_ingresso, semestre_ingresso, cpf_cifrado NULL, situacao)

arquivo            (id, storage_key, nome_original, mime, tamanho_bytes, sha256, paginas,
                    enviado_por, criado_em)

certificado        (id, aluno_id, arquivo_id, status,
                    titulo, instituicao, local, data_inicio, data_fim,
                    horas_certificado NUMERIC(6,2),
                    item_id,
                    horas_integralizadas NUMERIC(6,2),      -- snapshot no momento da confirmação/deferimento
                    origem_classificacao,                   -- REGRA | IA | MANUAL
                    confianca NUMERIC(4,3) NULL,
                    nome_confere BOOLEAN NULL,
                    duplicado_de NULL REFERENCES certificado,
                    confirmado_em, deferido_em, deferido_por,
                    versao, excluido_em NULL, criado_em, atualizado_em,
                    UNIQUE(aluno_id, arquivo_sha256) WHERE excluido_em IS NULL)

extracao           (id, certificado_id, metodo,            -- TEXTO | OCR
                    texto_bruto, campos_extraidos JSONB, sugestoes_item JSONB,   -- top-3 + confiança + justificativa
                    motor_classificacao, versao_motor, duracao_ms, erro NULL, criado_em)

avaliacao          (id, certificado_id, avaliador_id, decisao,  -- DEFERIDO | INDEFERIDO | AJUSTE_SOLICITADO | REABERTO
                    justificativa, item_anterior, item_novo, horas_anteriores, horas_novas, criado_em)

requerimento       (id, aluno_id, numero, gerado_em, gerado_por, total_horas, arquivo_pdf_id, arquivo_xlsx_id)

auditoria          (id, entidade, entidade_id, acao, antes JSONB, depois JSONB,
                    usuario_id, ip, criado_em)                 -- append-only
event_publication  (tabela do Spring Modulith)
spring_session*    (tabelas do Spring Session)
```

Índices iniciais: `certificado(aluno_id, status)`, `certificado(item_id)`, `certificado(status, criado_em)` para a fila, GIN `pg_trgm` em `certificado.titulo` para duplicidade aproximada, `auditoria(entidade, entidade_id)`.

Dados sensíveis: `cpf_cifrado` com criptografia de aplicação (AES‑GCM, chave fora do banco). `texto_bruto` da extração contém dados pessoais — política de retenção (ex.: apagar após deferimento + 1 ano, mantendo só os campos estruturados).

---

## 9. Motor de cálculo

Implementado como função pura no módulo `calculo`:

```java
SaldoHoras calcular(Regulamento regulamento, List<CertificadoParaCalculo> certificados);
```

Passos:
1. **Horas brutas por certificado**, conforme a estratégia do item:
   - `FIXO_POR_OCORRENCIA` → `horas_por_ocorrencia` (comportamento atual da planilha);
   - `HORAS_CERTIFICADO` → `horas_certificado`;
   - `HORAS_CERTIFICADO_COM_TETO` → `min(horas_certificado, teto_por_ocorrencia)`.
2. **Limite de ocorrências por item** (ex.: máx. 2 mandatos de representante) — ordena por data e descarta excedentes.
3. **Limite de horas por item**, depois **por macroatividade**.
4. Totais: integralizadas (DEFERIDO), pendentes (CONFIRMADO/AJUSTE_SOLICITADO), faltantes, projeção, percentual.
5. Retorna também o **detalhamento**: por certificado, quanto contou e quanto foi cortado e por qual regra (explicável ao aluno: "10 h não contabilizadas — limite de 40 h em Cidadania atingido").

Teste de aceite obrigatório: **o exemplo da planilha recalculado pelo motor deve dar exatamente 430 h** (13 certificados, estratégia fixa, sem limites).

O snapshot `horas_integralizadas` no certificado é gravado no deferimento; se o regulamento mudar, alunos já vinculados à versão anterior continuam sendo calculados por ela (RF05).

---

## 10. Pipeline de extração e classificação do PDF

Módulo `extracao`, executado de forma assíncrona (virtual threads, fila persistida via Event Publication Registry; limite de concorrência configurável).

### 10.1 Etapas

1. **Validação do arquivo**: tipo real pelo *magic number* (não pela extensão), tamanho, PDF sem senha, nº de páginas ≤ 5, sanitização (rejeitar PDFs com JavaScript embutido).
2. **Hash SHA‑256** → se já existe para o aluno, marca como duplicado e avisa.
3. **Texto**: Apache PDFBox. Se o texto útil < ~50 caracteres (certificado escaneado/imagem) → renderiza páginas a 300 dpi e passa no **Tesseract (por)** via Tess4J. Imagens JPG/PNG vão direto para OCR.
4. **Normalização**: remover acentos para busca, unificar espaços, corrigir hifenização de quebra de linha.
5. **Extração de campos** (heurísticas determinísticas, testadas com casos reais):
   - **Carga horária**: padrões como `carga horária (total)? de 20 horas`, `20h`, `20 h/a`, `20 (vinte) horas`, `duração: 4h30` (→ 4,5), `40 horas‑aula`; se houver vários números, prioriza o que está perto de "carga horária"/"duração"/"total". Números por extenso convertidos.
   - **Datas**: `dd/mm/aaaa`, `12 a 14 de maio de 2023`, `maio de 2023`; período início/fim.
   - **Instituição**: lista conhecida (Universidade de Vassouras/Univassouras, Alura, Udemy, Coursera, Fundação Bradesco, SEBRAE, Senai, DIO, Cisco NetAcad…) + heurística de cabeçalho.
   - **Nome do participante**: busca do nome do aluno (normalizado, tolerante a abreviações) → `nome_confere`.
   - **Título**: texto entre "participou do/da/de" / "concluiu o curso" e o próximo delimitador.
6. **Classificação do item (1–52)**, em camadas:
   - **Camada 1 — regras**: palavras‑chave e padrões por item (`palavras_chave` no catálogo, editáveis pelo admin). Ex.: "monitoria" → 18; "ENADE" + "simulado" → 13; "Alura/Udemy/Coursera" + "curso" → 46 (externo) ou 10 (se oferecido pelo curso); "palestra" + instituição externa → 48; "visita técnica" → 39 ou 50 (fora do RJ). Gera score por item.
   - **Camada 2 — IA: descartada (D7 = não, 11/11/2026).** Registro da ideia original, que não será implementada: envia **apenas o texto já sem CPF/RG** + catálogo de itens (nome, descrição, exemplos) a um LLM (ex.: Claude Haiku/Sonnet via API, integrado por Spring AI ou SDK Java oficial) pedindo resposta em **JSON estruturado**: `{item, confianca, alternativas[2], horas_certificado, justificativa}`. Timeout curto, retry, e cache por hash.
   - **Combinação**: se regras e IA concordam → confiança alta; se discordam → mostra as duas como alternativas e marca confiança baixa (vai primeiro na fila do coordenador).
   - Sem IA, o sistema funciona: regras sugerem, aluno confirma.
7. **Cálculo** das horas integralizadas daquele certificado (motor §9) para exibir na confirmação.
8. Persistência em `extracao` (o que foi lido e sugerido) e certificado → `AGUARDANDO_CONFIRMACAO`.

### 10.2 Qualidade medida, não suposta

- Pasta `samples/` com **≥ 50 certificados reais anonimizados** e o gabarito (horas + item corretos) em um CSV.
- Teste automatizado que roda o pipeline nesse conjunto e reporta **acurácia de horas** e **acurácia de item (top‑1 e top‑3)**; o CI falha se cair abaixo do patamar acordado.
- Em produção, o dashboard da coordenação mostra a **taxa de correção** (sugerido ≠ confirmado) por item → indica onde ajustar palavras‑chave/descrições.

---

## 11. API REST (resumo)

Base `/api/v1`, JSON, erros em **RFC 9457 (Problem Details)**, paginação `page/size/sort`, documentação springdoc em `/api/docs` (só em dev/homolog).

| Método | Rota | Perfil | Descrição |
|---|---|---|---|
| POST | `/auth/cadastro` | público | cadastro de aluno |
| POST | `/auth/login` · `/auth/logout` | público/autenticado | sessão |
| POST | `/auth/senha/esqueci` · `/auth/senha/redefinir` | público | recuperação |
| GET | `/me` | autenticado | usuário atual + perfil |
| GET | `/regulamentos/vigente` | autenticado | catálogo do aluno (macro + itens) |
| POST | `/certificados` (multipart, N arquivos) | ALUNO | upload → 202 + ids |
| GET | `/certificados?status=` | ALUNO | meus certificados |
| GET | `/certificados/{id}` | dono/coord | detalhe + extração + sugestões |
| GET | `/certificados/{id}/arquivo` | dono/coord | PDF (URL pré‑assinada de curta duração) |
| GET | `/certificados/{id}/eventos` (SSE) | dono | progresso do processamento |
| PUT | `/certificados/{id}` | ALUNO | editar campos |
| POST | `/certificados/{id}/confirmar` | ALUNO | confirma horas e item |
| DELETE | `/certificados/{id}` | ALUNO | exclusão lógica (se não deferido) |
| POST | `/certificados/simular` | ALUNO | "se eu enviar um certificado do item X com Y h, quanto conta?" |
| GET | `/alunos/me/saldo` | ALUNO | contadores + detalhamento |
| GET | `/coordenacao/fila` | COORD | fila de análise com filtros |
| POST | `/coordenacao/certificados/{id}/decisao` | COORD | deferir/indeferir/ajuste |
| POST | `/coordenacao/certificados/decisao-em-lote` | COORD | lote |
| GET | `/coordenacao/alunos` · `/coordenacao/alunos/{id}` | COORD | lista e ficha do aluno |
| POST | `/coordenacao/alunos/{id}/requerimento` | COORD | gera PDF/XLSX oficial |
| GET | `/dashboards/aluno` · `/dashboards/coordenacao` | por perfil | dados agregados |
| GET | `/relatorios/export?formato=xlsx` | COORD | exportação |
| CRUD | `/admin/regulamentos`, `/admin/itens`, `/admin/cursos`, `/admin/usuarios` | ADMIN | gestão |
| POST | `/admin/importacao/planilha` | ADMIN | importa XLSX legado |

---

## 12. Telas (frontend)

**Aluno**
1. Login / cadastro / recuperar senha.
2. **Início (dashboard)**: anel de progresso (integralizadas / exigidas), cartões *Tenho · Pendentes · Faltam*, barras por macroatividade (com limite), últimos certificados, alertas (duplicado, ajuste solicitado).
3. **Enviar certificados**: drag‑and‑drop múltiplo, câmera no celular, progresso por arquivo.
4. **Confirmação** (a tela mais importante): PDF ao lado, campos pré‑preenchidos, item sugerido + alternativas, "conta X h", pergunta "Está correto?" → Salvar / Corrigir. Em lote: navegar entre os pendentes de confirmação.
5. **Meus certificados**: tabela filtrável por status/item/macro; edição.
6. **Regulamento**: catálogo pesquisável com o que cada item aceita e quanto vale; simulador.

**Coordenação**
7. Dashboard da coordenação (§13).
8. **Fila de análise**: lista ordenada (baixa confiança e mais antigos primeiro), visualização do PDF, atalhos de teclado (D = deferir, I = indeferir, A = ajuste), lote.
9. Ficha do aluno: saldo, histórico, certificados, gerar requerimento.

**Admin**
10. Regulamentos e itens (editor com versão e publicação), cursos, usuários/papéis, importação de planilha, configurações (IA ligada/desligada, limites de upload).

Design: componentes shadcn/ui, tema claro/escuro, mobile‑first para as telas do aluno.

---

## 13. Dashboards

### Aluno
- Progresso total (%), horas *integralizadas / pendentes / faltantes*, projeção.
- Horas por macroatividade e por item (com teto e quanto falta para o teto).
- Linha do tempo de horas acumuladas por semestre.
- Sugestão de onde ainda há "espaço" (itens/macros com limite não atingido).

### Coordenação
- KPIs: nº de alunos, % que já completou, média de horas por período/ano de ingresso, certificados aguardando análise, tempo médio até decisão.
- **Alunos em risco**: períodos finais com saldo baixo (lista exportável).
- Distribuição de horas por macroatividade/item (o que os alunos mais fazem).
- Taxa de deferimento/indeferimento por item.
- Qualidade da automação: % de sugestões aceitas sem correção, por item.
- Filtros: curso, ano/semestre de ingresso, período, status.
- Exportação CSV/XLSX de qualquer visão.

Implementação: consultas agregadas em SQL (views ou consultas nativas no módulo `relatorios`); se ficar pesado, *materialized views* atualizadas por evento/agendamento. Não precisa de ferramenta de BI externa.

---

## 14. Segurança e LGPD

- **Autenticação**: sessão server‑side (Spring Session JDBC), cookie `HttpOnly; Secure; SameSite=Lax`, proteção CSRF com token para SPA. Evita JWT no navegador (mais simples e mais seguro para SPA de mesma origem). OIDC (Google/Microsoft da universidade) plugável.
- **Senhas**: Argon2id (ou bcrypt), política mínima, bloqueio progressivo após tentativas (rate‑limit com Bucket4j).
- **Autorização**: `@PreAuthorize` + checagem de posse/escopo de curso no serviço; testes de autorização para cada endpoint (aluno A não lê certificado do aluno B).
- **Uploads**: validação por conteúdo, tamanho, nomes aleatórios no storage, download só por URL pré‑assinada de curta validade, bucket privado, (opcional) antivírus ClamAV.
- **Headers**: CSP restritiva, HSTS, X‑Content‑Type‑Options, Referrer‑Policy (configurados no Caddy).
- **Segredos**: variáveis de ambiente / secret manager; nunca no repositório.
- **Dependências**: Dependabot/Renovate + OWASP Dependency‑Check / `npm audit` no CI.
- **LGPD**: base legal e termo de uso no cadastro; consentimento específico para processamento por IA (se D7 = sim); minimização (sem RG; CPF opcional e cifrado); direito de acesso/exportação/exclusão; retenção definida para texto bruto e arquivos; auditoria de acesso a dados de terceiros (coordenador vendo aluno); encarregado (DPO) da universidade informado.

---

## 15. Infraestrutura, ambientes e operação

| Ambiente | Uso | Como |
|---|---|---|
| local | desenvolvimento | `docker compose up` (Postgres, MinIO, Mailpit) + app pela IDE; Spring Boot DevTools; Testcontainers |
| homolog | validação com coordenação | deploy automático a cada merge em `main` |
| produção | uso real | deploy por tag/release aprovada |

- **Deploy**: Docker Compose em uma VM (2 vCPU / 4 GB é suficiente para começar) com Caddy + backend + (Postgres gerenciado ou container com volume). Sem Kubernetes.
- **Backups**: `pg_dump` diário cifrado para outro local + retenção 30 dias; versionamento no bucket; **teste de restore mensal** documentado.
- **Monitoramento**: health checks (Actuator), alertas de erro (Sentry), métricas de fila de extração (tamanho, falhas, latência), uptime externo.
- **Configuração**: `application.yml` por perfil + variáveis de ambiente; feature flags simples em tabela/config (ex.: `extracao.ia.habilitada`).

---

## 16. Qualidade e testes

| Camada | Ferramentas | O que cobre |
|---|---|---|
| Unitário | JUnit 5, AssertJ | motor de cálculo (inclui o caso 430 h), máquina de estados, parsers de horas/datas, regras de classificação |
| Arquitetura | Spring Modulith `verify()`, ArchUnit | fronteiras entre módulos, sem ciclos, domínio sem dependência de web |
| Integração | `@SpringBootTest` + **Testcontainers** (Postgres, MinIO) | repositórios, migrações Flyway, upload ponta a ponta, eventos |
| Segurança | Spring Security Test | matriz de autorização por endpoint |
| Extração | conjunto `samples/` + gabarito | acurácia de horas e item, com patamar mínimo no CI |
| Frontend | Vitest + Testing Library | componentes, formulários, validação |
| E2E | Playwright | cadastro → upload → confirmação → deferimento → saldo atualizado |
| Estática | Spotless (formatação), Error Prone + NullAway (JSpecify), ESLint, TypeScript strict | consistência e nulidade |
| Cobertura | JaCoCo | meta ≥ 80% em `calculo`, `certificados`, `extracao` |

Processo: trunk‑based com PRs curtos, CI obrigatório verde, revisão de código, Conventional Commits, changelog gerado, versionamento semântico.

---

## 17. Migração da planilha

1. **Seed do catálogo** (Apêndice A) via migração Flyway `V2__seed_regulamento_engenharias.sql`, já com as correções do §1.3 (item 34, descrições 37 e 52, typos) **após validação da coordenação**.
2. **Importador** (Apache POI) da aba DESCRITIVO: para cada linha preenchida cria um certificado com `item`, `titulo`, `local`, `horas_certificado`, status `DEFERIDO` (ou `CONFIRMADO`, a decidir), `origem = IMPORTACAO`, sem arquivo anexado (aluno pode anexar depois).
3. Relatório de conferência: total da planilha (`G67`) × total calculado pelo sistema — devem bater (ex.: 430 h).

---

## 18. Roadmap

Estimativas para **1 desenvolvedor sênior em dedicação parcial‑alta**; ajustar conforme disponibilidade. Cada fase termina com algo utilizável e demonstrável.

| Fase | Duração | Entregas | Critério de aceite |
|---|---|---|---|
| **F0 — Fundação** | 1 sem | monorepo, CI, Docker Compose, Spring Boot + React esqueleto, Flyway, ADRs iniciais, ambiente de homolog | pipeline verde; deploy de "hello" em homolog |
| **F1 — MVP sem automação** | 2–3 sem | contas e perfis; catálogo/regulamento seed; lançamento **manual** de certificado com upload do PDF; motor de cálculo; contadores tenho/faltam; lista de certificados | aluno lança os 13 certificados do exemplo e vê **430 h**; testes do motor verdes |
| **F2 — Extração** | 2 sem | pipeline assíncrono, PDFBox + OCR, extração de horas/datas/nome, duplicidade, tela de confirmação com PDF lado a lado | ≥ 90% de acerto de horas no conjunto `samples/` |
| **F3 — Classificação** | 1–2 sem | regras por palavra‑chave, (opcional) IA com JSON estruturado, top‑3 + confiança, métricas sugerido×confirmado | ≥ 80% top‑1 / ≥ 95% top‑3 no conjunto de amostras (meta inicial, revisar com dados) |
| **F4 — Coordenação** | 2 sem | fila, deferimento (individual e lote), ajustes, auditoria, e‑mails, requerimento PDF/XLSX oficial | coordenador defere o caso exemplo e gera o requerimento idêntico ao modelo |
| **F5 — Dashboards** | 1–2 sem | dashboard do aluno e da coordenação, exportações | KPIs conferidos com SQL manual |
| **F5.1 — Ajustes** | 1 sem | pedidos de 11/11/2026 (§18, "Fase 5.1"): cadastro simplificado, 4 engenharias, senha, regulamento compacto, observação no envio, termos validado/reprovado, ficha do aluno completa, triagem automática | responsável aprova as telas |
| **F6 — Produção** | 1–2 sem | importador da planilha, LGPD (termos, exportação/exclusão), hardening, backups + restore testado, observabilidade, piloto com 1 turma | piloto rodando, feedback coletado, checklist de segurança fechado |

Total estimado: **10–14 semanas** até o piloto.

---

### Fase 5.1 — ajustes antes da produção (pedidos de 11/11/2026)

**Concluída em 11/11/2026.** Todos os itens abaixo foram implementados; a triagem usa as 11 regras aprovadas. Decisões tomadas na implementação: o ingresso ficou opcional e é informado pela coordenação na ficha do aluno; o requerimento oficial mantém "Deferido em".

**Cadastro e login**
1. O cadastro pede só nome, e-mail, senha, matrícula e curso. Sai o ano e o semestre de ingresso.
   - *Impacto:* o período do aluno e a lista de "alunos em atenção" do painel usam o ingresso. Será preciso outra
     fonte (por exemplo, a coordenação informa o período, ou ele é deduzido da matrícula) ou retirar esse indicador.
2. Os cursos são só engenharias: **Engenharia Civil, Engenharia Elétrica, Engenharia Química e Engenharia de
   Software**. Hoje existe um único curso, "Engenharia"; a migração precisa definir para onde vão os alunos já
   cadastrados nele.
3. O e-mail é livre, institucional ou não. Trocar o rótulo "E-mail institucional" e não restringir domínio. Isso
   responde parte da D6.
4. O login é próprio (e-mail e senha) por enquanto. Login institucional fica para depois. Isso também responde a D6.
5. A senha precisa de no mínimo 8 caracteres, com pelo menos uma letra e um caractere especial. Hoje são 10
   caracteres sem outras exigências. A regra deve valer no backend e aparecer como dica no formulário.

**Telas do aluno**
6. A página do regulamento precisa usar o espaço melhor: hoje está grande e exige rolar muito. Opções: lista
   compacta por macroatividade, com a descrição recolhível e uma busca.
7. Depois de "Salvar alterações" o aluno precisa ver claramente o que aconteceu, com uma notificação do tipo
   "Certificado enviado para análise". Hoje ele é levado para "Meus certificados" sem perceber a mudança.
8. Ao enviar um certificado, campo opcional de **observação do aluno** (até 250 caracteres). A coordenação vê essa
   observação na análise.

**Análise pela coordenação**
9. A tela em que o coordenador de horas analisa o arquivo e valida (a fila de análise da F4) deve usar os termos do
   setor:
   - **Em análise**, **Validado** e **Reprovado**, no lugar de "deferido" e "indeferido" (a revisar com a equipe:
     o requerimento oficial usa "deferido");
   - ao reprovar ou pedir ajuste, o funcionário escreve o motivo e o que ficou faltando (já é obrigatório);
   - o aluno recebe a mensagem no site, além do e-mail: aviso no Início, selo de status e uma lista de notificações.
10. Os campos para corrigir o item e as horas ficam sempre visíveis na análise, sem a caixa "Corrigir o item ou as
    horas". O coordenador altera se quiser; a justificativa só é exigida se algo mudar.
11. Na aba Alunos, entrar no aluno e ver todos os certificados (com o arquivo) e os dados de horas: saldo, horas por
    macroatividade, por item e por semestre. A ficha atual da F4 é o ponto de partida.
12. **Triagem automática:** alertas por regras em cada certificado da fila (nome diferente do aluno, arquivo
    repetido etc.). As regras propostas estão aguardando a aprovação do responsável antes de implementar.

## 19. Riscos e mitigação

| Risco | Prob. | Impacto | Mitigação |
|---|---|---|---|
| Regra de cálculo/limites/total exigido indefinidos | alta | alto | responder §2 antes da F1; motor configurável por estratégia |
| Certificados muito heterogêneos (layouts, escaneados, fotos tortas) | alta | médio | OCR + confirmação humana obrigatória; medir com `samples/`; nunca deferir automaticamente |
| Classificação ambígua entre itens parecidos (ex.: 10×46, 14×40, 22×48, 39×50) | alta | médio | top‑3 + confiança; descrições/exemplos melhores no catálogo; coordenador decide |
| LGPD com IA externa | média | alto | IA opcional, texto pseudonimizado, consentimento, fallback por regras |
| Fraude (certificado editado) | média | médio | hash + duplicidade + revisão humana; (futuro) validação de QR/código de autenticidade dos emissores |
| Mudança de regulamento no meio do curso | média | médio | regulamento versionado por aluno |
| Dependência de uma só pessoa (manutenção) | média | alto | stack mainstream, ADRs, README de operação, testes como documentação viva |
| Hospedagem/custos indefinidos | média | médio | tudo em Docker, sem dependência de provedor |

---

## 20. Próximos passos imediatos

1. Responder as decisões **D1–D10** (§2) com a coordenação.
2. Validar o catálogo do Apêndice A (corrigir itens 34, 37, 52; descrever 6 e 7).
3. Reunir 50+ certificados reais (com autorização) e anonimizar para `samples/`, com gabarito de horas e item.
4. Iniciar **F0**: inicializar o repositório git, esqueleto backend/frontend e CI.

---

## Apêndice A — Catálogo de atividades (extraído da planilha, sem correções)

Regra atual: horas integralizadas = **horas por ocorrência × quantidade de certificados** no item.

| Nº | Macroatividade | Item | Horas por ocorrência | Descrição (original) |
|---|---|---|---|---|
| 1 | CIDADANIA | Atividades Culturais e Esportivas na Universidade | 10 | Eventos culturais e esportivos na universidade |
| 2 | CIDADANIA | Atividades em instituições de Ensino | 25 | Utilização da expertise de cada curso para a prestação de serviços para professores, pais e alunos em escolas públicas e particulars. |
| 3 | CIDADANIA | Atividades Nacionais de Cidadania | 5 | Eventos com a temática de Cidadania oferecidos em âmbito nacional na modalidade à distância. |
| 4 | CIDADANIA | Evento de Cidadania e Direitos Humanos | 20 | Evento que deve, preferencialmente, envolver todos os cursos da unidade para oferecer atendimento gratuito de saúde, educação e cidadania à população local, além de atividades de lazer e esporte. |
| 5 | CIDADANIA | Prestação de Serviços à Comunidade | 25 | Utilização da expertise de cada curso para o fornecimento de serviços que contribuam para o desenvolvimento das comunidades locais. |
| 6 | CIDADANIA | Presidente do DCE | 25 | — |
| 7 | CIDADANIA | Presidente do Centro Acadêmico | 25 | — |
| 8 | CIDADANIA | Ações Sócios Ambientais | 20 | É uma série de passeios históricos por bairros da região. Uma “aula ao vivo”, onde se é mostrada uma cidade moldada pelo seu conjunto arquitetônico e seus bens imateriais (personagens, acontecimentos, cheiros e sabores). São trabalhar os aspectos históricos, literários, sociológicos. |
| 9 | CIDADANIA | Trote Solidário | 10 | Recepção dos calouros com foco em cidadania e solidariedade. |
| 10 | CIENTÍFICO - ACADÊMICO | CURSOS LIVRES | 20 | Cursos oferecidos pelo curso fora da grade curricular |
| 11 | CIENTÍFICO - ACADÊMICO | Disiplinas Extracurriculares | 20 | Aprovação em disciplinas fora da matriz curricular |
| 12 | CIENTÍFICO - ACADÊMICO | Enade Entrega da Prova | 15 | Realização e entrega da prova do ENADE |
| 13 | CIENTÍFICO - ACADÊMICO | Enade Simulados Presenciais | 10 | Participação dos simulados do ENADE oferecidos pela Instituição online ou presencialmente |
| 14 | CIENTÍFICO - ACADÊMICO | Eventos Acadêmicos Científicos | 5 | Eventos oferecidos pela Universidade de Vassouras com o objetivo de incentivar a prática acadêmico-científica |
| 15 | CIENTÍFICO - ACADÊMICO | Grupo de Estudos | 5 | Grupos de alunos que se reunem periodicamente para estudar determinada disciplina ou assunto sob supervisão de um professor responsável |
| 16 | CIENTÍFICO - ACADÊMICO | Iniciação Científica | 50 | Programas de iniciação científica nas unidades com participação de alunos para prática acadêmica |
| 17 | CIENTÍFICO - ACADÊMICO | Intercâmbio | 50 | Realizaram Intercâmbio em convênio da Universidade de Vassouras. |
| 18 | CIENTÍFICO - ACADÊMICO | Monitoria | 50 | Alunos monitores de alguma disciplina ou projeto, dentro da política da Universidade de Vassouras. |
| 19 | CIENTÍFICO - ACADÊMICO | Nivelamentos | 10 | Participação nos nivelamentos oferecidos pela Universidade de Vassouras. |
| 20 | CIENTÍFICO - ACADÊMICO | Núcleos de Práticas Acadêmicas | 50 | Espaços para a prática dos conteúdos desenvolvidos em sala de aula (Não se confunde com estágio obrigatório) através da realização de atividades de cunho social ou não |
| 21 | CIENTÍFICO - ACADÊMICO | Oficinas Acadêmicas | 10 | Participação em oficinas acadêmicas em geral |
| 22 | CIENTÍFICO - ACADÊMICO | Palestras Presenciais | 5 | Participação em palestras oferecidas pela Universidade de Vassouras. |
| 23 | CIENTÍFICO - ACADÊMICO | Projeto Boas- Vindas | 10 | Participação nas ações de Boas-Vindas |
| 24 | CIENTÍFICO - ACADÊMICO | Projeto de Extensão Universitária | 50 | Participação em projetos de extensão universitária. |
| 25 | CIENTÍFICO - ACADÊMICO | Semana Acadêmica, Jornada Severino Sombra e ENIC | 5 | Evento específico direcionado a uma área ou a um curso (semana do curso). |
| 26 | CIENTÍFICO - ACADÊMICO | Trabalho Extraclasse | 5 | Trata-se dos trabalhos realizados pelos alunos fora do período de aula e que serão avaliados pelos professores de determinada disciplina sendo atribuídas aos alunos que obtiverem conceito satisfatório horas ao invés de pontos nas avaliações da disciplina |
| 27 | CAPACITAÇÃO PROFISIONAL, EMPREENDEDORISMO E INOVAÇÃO | Atividades de Empreendedorismo e Inovação | 25 | Atividades oferecidas pela Universidade de Vassouras com o objetivo de ampliar a visão do aluno a respeito dos campos de atuação profissional |
| 28 | CAPACITAÇÃO PROFISIONAL, EMPREENDEDORISMO E INOVAÇÃO | Atividades de Capacitação Profissional | 10 | Atividades oferecidas pela Universidade de Vassouras com o objetivo de preparar o aluno para processos seletivos e para mercado de trabalho |
| 29 | CAPACITAÇÃO PROFISIONAL, EMPREENDEDORISMO E INOVAÇÃO | Atividades Nacionais de Empreendedorismo e Inovação | 5 | Eventos com essa temática oferecidos em âmbito nacional na modalidade à distância |
| 30 | CAPACITAÇÃO PROFISIONAL, EMPREENDEDORISMO E INOVAÇÃO | Atividades Nacionais de Capacitação Profissional | 5 | Eventos com essa temática oferecidos em âmbito nacional na modalidade à distância |
| 31 | CAPACITAÇÃO PROFISIONAL, EMPREENDEDORISMO E INOVAÇÃO | Auxílio na Organização de Eventos | 10 | Participação do aluno como organizador de eventos da unidade sob supervisão de um professor Responsável |
| 32 | CAPACITAÇÃO PROFISIONAL, EMPREENDEDORISMO E INOVAÇÃO | Desafio Inovação | 10 | Desafio dentro do Ambiente Virtual de Aprendizagem com o tema Inovação para a participação dos alunos |
| 33 | CAPACITAÇÃO PROFISIONAL, EMPREENDEDORISMO E INOVAÇÃO | Encontro com Egressos | 10 | Encontro de ex-alunos com atividades para alunos da unidade e de escolas públicas. Essa atividade visa valorizar o egresso, fomentar o networking, estimular o alunado e apresentar para possíveis alunos o bom desempenho profissional de nossos egressos |
| 34 | CAPACITAÇÃO PROFISIONAL, EMPREENDEDORISMO E INOVAÇÃO | NTO DE EMPREENDEDORISMO E INOVAÇÃO | 5 | Evento sobre a temática oferecido pela Universidade de Vassouras com o objetivo de ampliar a visão do aluno a respeito dos campos de atuação profissional |
| 35 | CAPACITAÇÃO PROFISIONAL, EMPREENDEDORISMO E INOVAÇÃO | OLIMPÍADAS E CONCURSOS | 20 | Competições locais ou nacionais para incentivar a cultura do estudo e desenvolver competências e habilidades. |
| 36 | CAPACITAÇÃO PROFISIONAL, EMPREENDEDORISMO E INOVAÇÃO | Representante de Turma | 20 | Atuação como representante de turma |
| 37 | CAPACITAÇÃO PROFISIONAL, EMPREENDEDORISMO E INOVAÇÃO | Consu/ Consepe | 20 | Atuação como representante de turma |
| 38 | CAPACITAÇÃO PROFISIONAL, EMPREENDEDORISMO E INOVAÇÃO | Viagens de Excursões Acadêmicas | 25 | Viagens organizadas e oferecidas pelas unidades que proporcionem o encontro do mundo acadêmico com o profissional |
| 39 | CAPACITAÇÃO PROFISIONAL, EMPREENDEDORISMO E INOVAÇÃO | Visitas técnicas | 10 | Visitas organizadas e oferecidas pela Universidade de Vassouras que proporcionem o encontro do mundo acadêmico com o profissional |
| 40 | ATIVIDADES COMPLEMENTARES EXTERNAS | Eventos Acadêmico-Científicos | 10 | Participação de congressos, seminários, simpósios, fóruns, mesas redondas, conferências e outros eventos do tipo com abrangência Nacional |
| 41 | ATIVIDADES COMPLEMENTARES EXTERNAS | Certificações na Área do Curso | 20 | Certificações na área do curso |
| 42 | ATIVIDADES COMPLEMENTARES EXTERNAS | Trabalho Publicado/Premiado | 20 | Trabalhos publicados/premiados em eventos científicos e trabalhos ganhadores de concursos |
| 43 | ATIVIDADES COMPLEMENTARES EXTERNAS | Apresentação de Trabalho em Evento Acadêmico-Científico | 20 | Apresentação de trabalho acadêmico em eventos científicos |
| 44 | ATIVIDADES COMPLEMENTARES EXTERNAS | Eventos Culturais Externos | 20 | Participação de eventos culturais diversos tais como: bienais, peças teatrais, cinema, museus, feiras culturais e saraus (ingresso) |
| 45 | ATIVIDADES COMPLEMENTARES EXTERNAS | Vivência Profissional na Área do Curso | 30 | Atividade profissional remunerada ou não, incluindo estágio e emprego (exceto estágio obrigatório) |
| 46 | ATIVIDADES COMPLEMENTARES EXTERNAS | Cursos Online e Presenciais | 30 | Cursos online e presenciais desvinculados à instituição, incluindo cursos profissionalizantes e de línguas |
| 47 | ATIVIDADES COMPLEMENTARES EXTERNAS | Trabalho Voluntário | 40 | Trabalho voluntário em instituições, processo eleitoral |
| 48 | ATIVIDADES COMPLEMENTARES EXTERNAS | Palestras Externas | 5 | Palestras fora da instituição mediante apresentação de certificado de participação |
| 49 | ATIVIDADES COMPLEMENTARES EXTERNAS | Competições Universitárias Nacionais e Internacionais | 25 | Participação em competições universitárias |
| 50 | ATIVIDADES COMPLEMENTARES EXTERNAS | Intercâmbio ou Visita Técnica Fora do Estado do Rio de Janeiro | 100 | Realização de intercâmbio sem convênio da Universidade de Vassouras. |
| 51 | ATIVIDADES COMPLEMENTARES EXTERNAS | Eventos Étnicos Raciais | 20 | Participação em eventos relativo à educação étnico-raciais |
| 52 | ATIVIDADES COMPLEMENTARES EXTERNAS | Projeto Rondon | 100 | Realização de intercâmbio sem convênio da Universidade de Vassouras |
