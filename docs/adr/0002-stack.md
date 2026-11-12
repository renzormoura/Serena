# ADR 0002 — Stack

- Status: aceito
- Data: 11/11/2026

## Decisão

| Camada | Escolha |
|---|---|
| Linguagem | Java 21 (LTS instalada no ambiente de desenvolvimento) |
| Framework | Spring Boot 4.1.2, Spring Modulith 2.1 |
| Banco | PostgreSQL 17, migrações com Flyway |
| Sessão | Spring Session JDBC |
| Frontend | React 19 + TypeScript + Vite, Tailwind CSS 4, TanStack Query, React Router |
| Testes | JUnit 5, AssertJ, Testcontainers, Vitest |

## Notas

- O plano previa Java 25. A máquina de desenvolvimento tem Java 21, e o código não usa nada além do 21. Migrar
  para 25 é mudar `java.version` no `pom.xml` e a versão no CI.
- Os tipos TypeScript da API são gerados do contrato OpenAPI (springdoc). O teste `ContratoOpenApiTest` falha se
  `docs/api/openapi.json` não bater com a API real; o CI do frontend falha se `esquema.d.ts` não bater com o
  contrato. Campos de resposta que podem vir nulos são marcados com `@Anulavel`; os demais são obrigatórios.
- `openapi-typescript` roda isolado via `npx` com TypeScript 5, porque ainda não aceita o TypeScript 7 do projeto.
