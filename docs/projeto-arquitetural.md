# SaúdeCerta — E2 · Projeto Arquitetural

> IFPA · TADS · TEADS0347 – Desenvolvimento de Aplicações Multicamadas · Turma C795NL · 2026.2
> Docente: Márcio Goes do Nascimento
> Equipe: João Oliveira · [Integrante 2] · [Integrante 3] · [Integrante 4]
> Versão 1.0 — 10/11/2026 · Base: E1 (Documento de Visão e Requisitos, v1.0)

Esta entrega transforma os requisitos e drivers da E1 em arquitetura. Os identificadores (RF, RN, RNF, DA, RT) são os mesmos da E1, então toda decisão aqui pode ser rastreada até um requisito.

## Conteúdo

| # | Documento | O que responde |
|---|---|---|
| 1 | [Diagramas C4](01-diagramas-c4.md) | Contexto, Contêiner e Componente, com descrição de cada elemento |
| 2 | [Camadas, componentes e modelo de domínio](02-camadas-componentes-dominio.md) | Responsabilidade de cada camada, regras de dependência, pacotes, API REST, agregados e onde cada regra de negócio é garantida |
| 3 | [Cenários de qualidade](03-cenarios-de-qualidade.md) | Cenários de seis partes para os principais RNFs, com tática e forma de verificar |
| 4 | [ADRs](adr/) | Seis decisões com alternativas, tradeoffs e custo/benefício |
| 5 | [Matriz de rastreabilidade](04-matriz-rastreabilidade.md) | Requisito → driver → ADR → componente → verificação |

### Registros de decisão (ADRs)

| ADR | Decisão | Drivers |
|---|---|---|
| [ADR-001](adr/ADR-001-monolito-modular-hexagonal.md) | Monólito modular com camadas hexagonais e fronteiras verificadas por Spring Modulith | DA3, DA7, RT02 |
| [ADR-002](adr/ADR-002-auditoria-assincrona-eventos.md) | Auditoria por eventos de domínio, entregues por outbox transacional e gravados com hash encadeado | DA2, DA4 |
| [ADR-003](adr/ADR-003-autenticacao-sessao-rbac.md) | Sessão no servidor (Spring Session JDBC) + RBAC no caso de uso e menor privilégio do admin | DA1, DA6 |
| [ADR-004](adr/ADR-004-concorrencia-marcacao.md) | Conflito de agenda: regra no domínio + índice único de “slot ativo” no banco | DA7 |
| [ADR-005](adr/ADR-005-prontuario-somente-anexacao.md) | Prontuário somente-anexação com adendos, protegido em três camadas | DA2, DA1 |
| [ADR-006](adr/ADR-006-estrategia-persistencia.md) | Persistência: schema por módulo, Flyway e mapeamento JPA conforme a complexidade do módulo | DA3, DA4 |

## Diagramas (.drawio)

Os diagramas editáveis estão em [`diagramas/`](diagramas/) e abrem no [draw.io / diagrams.net](https://app.diagrams.net) (Arquivo → Abrir) ou na extensão *Draw.io Integration* do VS Code. As imagens em `diagramas/png/` são exportações dos mesmos arquivos, usadas nos documentos para visualização no GitHub.

| Arquivo | Páginas |
|---|---|
| `c4-contexto.drawio` | C4 nível 1 |
| `c4-conteiner.drawio` | C4 nível 2 |
| `c4-componente.drawio` | C4 nível 3 — (1) componentes da API por módulo; (2) detalhe hexagonal do módulo Agenda |
| `modelo-dominio.drawio` | Modelo de domínio (classes, agregados e relacionamentos) |

## Sugestão de organização no repositório

```
saudecerta/
├── docs/
│   └── arquitetura/        ← conteúdo desta entrega
│       ├── README.md
│       ├── 01-diagramas-c4.md ...
│       ├── adr/
│       └── diagramas/
└── src/main/java/br/edu/ifpa/saudecerta/...
```

Os ADRs ficam versionados junto do código (RT04). Quando uma decisão mudar, cria-se um novo ADR que *substitui* o anterior; o antigo não é apagado, só muda o status.

## O que mudou desde a E1

- A escolha de estilo (monólito modular hexagonal) foi formalizada no ADR-001, agora com o mecanismo de fiscalização das fronteiras (Spring Modulith + ArchUnit).
- A questão em aberto da E1 sobre perda de evento de auditoria foi resolvida no ADR-002 (outbox via *Event Publication Registry*).
- Decidimos **sessão no servidor** em vez de JWT (ADR-003), porque bloquear um usuário (RF16) precisa ter efeito imediato e escalar horizontalmente não é driver (E1, seção 7.1).
- O modelo de domínio foi refinado em agregados: `Atendimento` passou a ser raiz de agregado própria, e não mais um filho carregado dentro de `Prontuario`, para que abrir o histórico não exija carregar o prontuário inteiro (DA4).
