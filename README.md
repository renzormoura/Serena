# Sereno

**Horas complementares das Engenharias da Universidade de Vassouras, sem planilha.**

O aluno envia o certificado (PDF ou foto). O Sereno lê o arquivo, sugere a carga horária e o item do regulamento, e o
aluno confere. A coordenação analisa com a ajuda de uma triagem automática, valida ou reprova e gera o requerimento
oficial em PDF e XLSX. O sistema substitui a planilha `HORAS COMPLEMENTARES - Modelo Engs.xlsx`.

![Java 21](https://img.shields.io/badge/Java-21-7b1e2e)
![Spring Boot 4](https://img.shields.io/badge/Spring%20Boot-4.1-7b1e2e)
![React 19](https://img.shields.io/badge/React-19-7b1e2e)
![PostgreSQL 17](https://img.shields.io/badge/PostgreSQL-17-7b1e2e)

---

## Funcionalidades

### Aluno

- **Cadastro e login:** cadastro com nome, e-mail, senha, matrícula e curso (Engenharia Civil, Elétrica, Química ou
  de Software). O login aceita o e-mail ou a matrícula.
- **Envio de certificados:** até 10 arquivos por vez (PDF, JPG ou PNG, até 10 MB). Arquivos duplicados são recusados.
- **Leitura automática:** o sistema lê o texto do PDF ou, nas fotos e PDFs escaneados, usa OCR. Ele sugere carga
  horária, datas, nome da atividade, instituição e o item do regulamento, e confere se o nome do aluno aparece.
- **Conferência:** o arquivo fica ao lado do formulário, e o valor "este certificado vai contar X h" é calculado ao
  vivo. O aluno pode deixar uma observação para a coordenação.
- **Saldo e painel:** horas validadas, em análise e faltantes, por macroatividade, por item e por semestre.
- **Avisos:** resultado de cada análise (validado, reprovado, ajuste pedido) no site e por e-mail, com a mensagem da
  coordenação.
- **Requerimento oficial:** o aluno gera o requerimento com as horas já validadas, em PDF e XLSX.
- **Regulamento:** os 52 itens em formato compacto, com busca.

### Coordenação

- **Painel:**
  - indicadores: alunos, quantos já integralizaram, certificados na fila e tempo médio de análise;
  - gráficos: faixas de progresso e itens mais usados;
  - alunos em atenção e acerto da leitura automática;
  - filtros por curso e ano de ingresso, e exportação XLSX.
- **Fila de análise com triagem automática:** a fila separa os certificados "com alertas" dos "sem alertas". Os sem
  alertas podem ser validados em lote.
- **Análise:**
  - o arquivo fica ao lado dos dados e da sugestão do sistema;
  - item e horas podem ser corrigidos;
  - **Validar**, **Pedir ajuste** e **Reprovar**, com os atalhos `V`, `A` e `R`;
  - qualquer correção ou reprovação exige uma mensagem para o aluno;
  - histórico completo de decisões;
  - reabertura e exclusão de certificado, com motivo.
- **Alunos:** lista com busca e ficha completa de cada aluno: saldo, horas por macroatividade, item e semestre, e os
  certificados com o arquivo.
- **Requerimento oficial:**
  - mesmo layout da aba INTEGRALIZAÇÃO da planilha;
  - com a data real do deferimento e o nome de quem deferiu;
  - cada geração fica guardada e numerada.
- **Equipe:** criação de contas de coordenadores (por curso) e de administradores.

O coordenador só enxerga os alunos dos cursos a que está vinculado. Essa checagem é feita no servidor, em toda
operação.

### Triagem automática (sem IA)

A triagem aplica 11 regras a cada certificado da fila. Ela só aponta: quem valida ou reprova é sempre a coordenação.

| Gravidade | Alertas |
|---|---|
| **Forte** | nome do aluno não aparece no certificado · mesmo arquivo enviado por outra conta · certificado repetido com outro arquivo · horas informadas diferentes das lidas no arquivo |
| **Atenção** | item trocado pelo aluno · arquivo ilegível · data suspeita (sem data, muito antiga ou anterior ao ingresso) · carga horária fora do comum · limite do item atingido · texto que não parece certificado |
| **Informação** | PDF que passou por um editor |

A sugestão do item e a triagem usam só regras: palavras-chave do regulamento, origem da atividade, metadados do PDF.
Nenhum dado de aluno é enviado para serviços externos.

---

## Stack

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Modulith (monólito modular), Spring Security com sessão no servidor (Spring Session JDBC), JPA, Flyway |
| Leitura de arquivos | Apache PDFBox, Tesseract (Tess4J) com o modelo de português |
| Documentos | Apache POI (XLSX), OpenPDF (PDF) |
| Banco | PostgreSQL 17 |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS 4, TanStack Query, React Router |
| Contrato | OpenAPI (springdoc); os tipos do frontend são gerados do contrato |
| Testes | JUnit 5, Testcontainers, Awaitility, Vitest |

### Módulos do backend

```
identidade     contas, papéis, sessão e segurança HTTP
academico      cursos, alunos, coordenação por curso, equipe
regulamento    regulamento versionado: macroatividades, itens, limites, palavras-chave
calculo        motor de cálculo de horas (puro, sem infraestrutura)
extracao       leitura de PDF, OCR e extração de campos
classificacao  sugestão do item por regras
certificados   ciclo de vida do certificado, análise, triagem, painel
requerimento   requerimento oficial em PDF e XLSX
notificacao    avisos no site e por e-mail
comum          erros (Problem Details) e utilitários
```

As fronteiras entre módulos são verificadas por teste (`ModularidadeTest`).

---

## Rodar localmente

**Pré-requisitos:** Java 21, Node 24 e Docker.

1. Suba o banco e o servidor de e-mail de testes (Mailpit):

   ```bash
   docker compose -f infra/docker-compose.yml up -d
   ```

2. Inicie o backend (porta 8080) com o perfil `dev`:

   ```bash
   cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

3. Inicie o frontend (porta 5173):

   ```bash
   cd frontend && npm install && npm run dev
   ```

Abra **http://localhost:5173**.

### Contas de teste (perfil `dev`)

| Perfil | Login | Senha |
|---|---|---|
| Aluno | `aluno@dev.local` ou matrícula `DEV0001` | `sereno-dev` |
| Coordenação (as quatro engenharias) | `coordenacao@dev.local` | `sereno-dev` |
| Administração | `admin@dev.local` | `sereno-dev` |

Além dessas, o sistema tem **contas fixas no código** (`ContasFixas`). Elas existem em qualquer ambiente,
inclusive em produção, e são recriadas ao subir se o banco for perdido: o administrador `adm@gmail.com`, a
coordenação `coordenacao@dev.local` (vinculada às quatro engenharias) e dois alunos, com as matrículas 202410568
e 202412748. Elas não seguem a regra de senha do cadastro.

A senha das contas de teste pode ser trocada com `SERENO_DEV_SENHA`. Os e-mails enviados aos alunos ficam no Mailpit, em
http://localhost:8025; nada sai da máquina.

### OCR (fotos e PDFs escaneados)

PDFs com texto são lidos sem instalar nada. Para ler imagens, baixe o modelo de português do Tesseract:

```bash
mkdir -p backend/.tessdata && curl -sSL -o backend/.tessdata/por.traineddata https://github.com/tesseract-ocr/tessdata_fast/raw/main/por.traineddata
```

- **Windows:** a biblioteca já vem com o Tess4J.
- **Linux:** instale também o pacote `tesseract-ocr`.

Sem o modelo, o OCR fica desligado e as imagens chegam para o aluno preencher manualmente.

---

## Configuração

| Variável | Para quê | Padrão |
|---|---|---|
| `SERENO_DB_URL`, `SERENO_DB_USER`, `SERENO_DB_PASSWORD` | Banco de dados | `localhost:5432/sereno` |
| `SERENO_ARQUIVOS_DIR` | Onde os arquivos enviados são guardados | `./dados/arquivos` |
| `SERENO_DOMINIOS_PERMITIDOS` | Domínios de e-mail aceitos no cadastro (separados por vírgula) | qualquer |
| `SERENO_COOKIE_SECURE` | Cookie de sessão só em HTTPS | `true` |
| `SERENO_OCR_TESSDATA` | Pasta com `por.traineddata` (liga o OCR) | desligado (`.tessdata` em `dev`) |
| `SERENO_EMAIL_ATIVO` | Envia avisos por e-mail (exige `SPRING_MAIL_HOST` etc.) | `false` (`true` em `dev`) |
| `SERENO_EMAIL_REMETENTE` | Remetente dos e-mails | `Sereno <nao-responda@univassouras.edu.br>` |
| `SERENO_URL_BASE` | Endereço do sistema, usado nos links dos e-mails | `http://localhost:5173` |

Ajustes de regra, em `application.yml`:
- `sereno.triagem.anos-maximos` (padrão 5) e `sereno.triagem.horas-maximas` (padrão 200): cortes da triagem;
- `sereno.painel.periodo-atencao` (padrão 8) e `sereno.painel.percentual-atencao` (padrão 50): critério de "alunos
  em atenção".

O total de horas exigidas é definido pela administração em
`PUT /api/v1/admin/regulamentos/vigente/horas-exigidas`.

---

## Testes

```bash
cd backend && ./mvnw verify
```

```bash
cd frontend && npm test
```

- Os testes de integração do backend usam Testcontainers (Postgres real) e são pulados sem Docker.
- Pare o backend de desenvolvimento antes do `verify`: a recompilação derruba o servidor em execução até ele ser
  reiniciado.

### Contrato da API

O contrato fica em [`docs/api/openapi.json`](docs/api/openapi.json), e um teste falha se ele ficar desatualizado. Ao
mudar a API de propósito, regenere o contrato e os tipos do frontend:

```bash
cd backend && ./mvnw test -Dtest=ContratoOpenApiTest -Dcontrato.atualizar=true
```

```bash
cd frontend && npm run api:tipos
```

---

## Deploy (Render ou qualquer servidor com Docker)

O `Dockerfile` da raiz gera uma imagem única: o backend serve a API e as telas no mesmo endereço, com o Tesseract
incluído para o OCR.

### No Render

1. Crie um **PostgreSQL** (New → PostgreSQL).
2. Crie um **Web Service** a partir deste repositório e escolha **Language: Docker**. Com isso não é preciso
   preencher Build Command nem Start Command.
3. Em **Environment Variables**, crie:

   | Variável | Valor |
   |---|---|
   | `SERENO_DB_URL` | `jdbc:postgresql://HOST:5432/BANCO`, com o host e o banco da "Internal Database URL" do Postgres |
   | `SERENO_DB_USER` | usuário do banco |
   | `SERENO_DB_PASSWORD` | senha do banco |
   | `SERENO_URL_BASE` | o endereço do serviço, por exemplo `https://seu-servico.onrender.com` |
   | `SERENO_ADMIN_EMAIL` | e-mail da primeira conta de administração |
   | `SERENO_ADMIN_SENHA` | senha dela (8 caracteres, uma letra e um especial) |

4. Em **Health Check Path**, use `/actuator/health`.

A primeira conta de administração é criada na primeira subida. Depois, ela cadastra a coordenação na tela
**Equipe**.

Não use o perfil `dev` em produção: ele cria as contas de teste com senha pública.

**Limitações do plano gratuito do Render:**
- o disco é apagado a cada deploy, então os arquivos enviados se perdem; para guardá-los, é preciso um disco
  persistente montado em `/app/dados`;
- o serviço dorme sem uso;
- o banco gratuito expira.

## Estrutura do repositório

```
backend/    Spring Boot (Java 21), monólito modular
frontend/   React + TypeScript + Vite
infra/      docker-compose para desenvolvimento (Postgres e Mailpit)
docs/       plano do projeto, decisões de arquitetura (ADRs) e contrato da API
```

- Plano completo, decisões e roadmap: [`docs/PLANO-DO-PROJETO.md`](docs/PLANO-DO-PROJETO.md)
- Decisões de arquitetura: [`docs/adr`](docs/adr)

## Situação

As fases F0 a F5 e os ajustes da fase 5.1 estão concluídos. A próxima fase (F6, produção) inclui:
- importação da planilha antiga;
- LGPD: termos de uso, exportação e exclusão de dados;
- endurecimento de segurança;
- backups com restauração testada;
- monitoramento;
- piloto com uma turma.

Pendências que dependem da coordenação estão no plano (decisões D1 a D10): total de horas exigidas, limites por
item e validação do requerimento com o modelo oficial.
