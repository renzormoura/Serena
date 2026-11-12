# ADR 0001 — Monólito modular com Spring Modulith

- Status: aceito
- Data: 11/11/2026

## Contexto

O volume esperado é de milhares de alunos e dezenas de milhares de certificados. Um único nó dá conta com folga.
A manutenção futura pode ficar com uma equipe pequena ou com uma pessoa só.

## Decisão

Uma única aplicação Spring Boot, dividida em módulos de topo (`identidade`, `regulamento`, `calculo`,
`certificados`, `extracao`, `avaliacao`, `relatorios`, `auditoria`, `notificacao`). Cada módulo expõe só o pacote
raiz; subpacotes são internos. Comunicação entre módulos por API pública ou eventos de domínio.

As fronteiras são verificadas em teste (`ModularidadeTest`, `ApplicationModules.verify()`), então uma dependência
errada quebra o build.

## Consequências

- Um deploy, um banco, operação simples.
- Separar um módulo em serviço próprio no futuro é possível, porque as fronteiras já existem.
- O módulo `calculo` é puro (sem Spring nem banco) e pode ser testado isoladamente.
