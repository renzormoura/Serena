# ADR 0003 — Sessão no servidor em vez de JWT

- Status: aceito
- Data: 11/11/2026

## Contexto

O SPA e a API são servidos na mesma origem (em dev pelo proxy do Vite; em produção pelo Caddy/Nginx).

## Decisão

Autenticação por sessão no servidor (Spring Session JDBC), cookie `SERENO_SESSION` com `HttpOnly`, `Secure` e
`SameSite=Lax`. Proteção CSRF no padrão SPA do Spring Security: cookie `XSRF-TOKEN` legível pelo JavaScript e
cabeçalho `X-XSRF-TOKEN` em toda requisição que altera estado.

## Por quê

- Nenhum token fica acessível ao JavaScript, então um XSS não consegue roubá-lo.
- Logout e revogação são imediatos (apagar a sessão), sem lista de bloqueio.
- Sessões sobrevivem a reinícios e permitem mais de uma instância, porque vivem no banco.

## Consequências

- Clientes não-navegador (apps, integrações) precisarão de outro mecanismo se surgirem.
- O login usa `application/x-www-form-urlencoded` em `POST /api/v1/auth/login` (campos `email` e `senha`),
  respondendo 204 ou 401.
