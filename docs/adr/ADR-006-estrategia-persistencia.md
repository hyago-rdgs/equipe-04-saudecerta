# ADR-006 — Persistência: schema por módulo, Flyway e mapeamento conforme a complexidade

| Campo | Valor |
|---|---|
| Status | **Aceito** |
| Data | 10/11/2026 |
| Decisores | Equipe SaúdeCerta |
| Drivers | DA3 Modificabilidade · DA4 Desempenho · RT03 H2/PostgreSQL · RT05 Extração no PF |
| Requisitos | RNF03, RNF05, RNF09, RN01, RN04 |

## Contexto

São três perguntas ligadas:

1. **Como dividir os dados entre módulos?** Se todos os módulos usam tabelas uns dos outros, a fronteira do ADR-001 vale só no código, e extrair a Agenda exigiria separar os dados à força.
2. **Como evoluir o schema?** Precisamos do mesmo schema em H2 (dev/testes) e PostgreSQL (produção), versionado no Git (RT04).
3. **O domínio deve carregar anotações JPA?** O guia lista “vazar JPA no domínio” como erro comum. Por outro lado, separar modelo de domínio e entidade JPA dobra classes e exige mapeadores, um custo real para uma equipe pequena.

## Alternativas

**Divisão dos dados**

| # | Alternativa | Prós | Contras |
|---|---|---|---|
| D1 | Um schema, tabelas de todos | Simples; FKs livres | Fronteira dos módulos invisível no banco; JOINs entre módulos aparecem “de graça” e acoplam |
| D2 | **Um banco, um schema por módulo, sem FK entre schemas** | Fronteira visível; extrair um módulo é mover um schema; um único backup | Integridade entre módulos é garantida pela aplicação (portas), não por FK |
| D3 | Um banco por módulo | Isolamento máximo | 6 bancos para operar; sem transação local entre Atendimento e Consulta (quebra o ADR-001) |

**Evolução do schema**

| # | Alternativa | Contras |
|---|---|---|
| M1 | `spring.jpa.hibernate.ddl-auto=update` | Não versionado, não revisável, perigoso em produção |
| M2 | **Flyway** com migrações SQL versionadas | Escrever SQL à mão (o que também é vantagem: revisável) |
| M3 | Liquibase | Equivalente, mais verboso; a equipe já conhece Flyway |

**Mapeamento do domínio**

| # | Alternativa | Prós | Contras |
|---|---|---|---|
| P1 | Entidades de domínio anotadas com JPA em todos os módulos | Menos código | O domínio passa a ter construtor vazio, *setters* para o Hibernate e *lazy loading*; enfraquece invariantes (`Consulta` com status alterável por reflexão) |
| P2 | Domínio puro + entidade JPA separada + mapeador, em todos os módulos | Domínio 100% isolado | +30–40% de classes; mapeamento trivial e repetitivo em módulos de CRUD |
| P3 | **Híbrido**: modelo separado onde há regra rica e invariantes; JPA direto onde o módulo é essencialmente cadastro ou leitura | Custo onde há benefício | Duas convenções no projeto, que precisam estar documentadas (este ADR) |

## Análise custo/benefício (mapeamento)

Estimativa de classes extras e benefício por módulo:

| Módulo | Regras de domínio | Classes extras com P2 | Benefício do isolamento | Escolha |
|---|---|---|---|---|
| Agenda | RN02, RN07–RN10, máquina de estados | ~6 | **Alto**: invariantes e candidato a extração | **Separado** |
| Prontuário | RN03, RN05, RN11, imutabilidade | ~6 | **Alto**: imutabilidade não pode depender do Hibernate | **Separado** |
| Segurança & Auditoria | hash encadeado, bloqueio | ~4 | Médio-alto | **Separado** |
| Cadastros | RN01, validação de CPF | ~6 | Baixo: quase CRUD | JPA no domínio |
| Prescrição/Exames | RN04, status do exame | ~5 | Baixo-médio | JPA no domínio |
| Relatórios | — (leitura) | — | Nenhum | Projeções `record` |

O P3 custa ~16 classes extras, onde o P2 custaria ~27, e preserva o isolamento nos dois módulos que importam para os drivers DA2, DA7 e RT05.

## Decisão

- **D2 — um PostgreSQL, um schema por módulo:** `cadastro`, `agenda`, `prontuario`, `prescricao`, `seguranca`, `auditoria`. FKs **só dentro** do schema. Entre módulos, colunas `*_id NOT NULL` com índice, mas sem FK. A integridade é verificada pela porta pública no caso de uso (ex.: `PrescricaoService` chama `AtendimentoApi.existe()` — RN04).
- **M2 — Flyway:** migrações em `db/migration/{vendor}` para separar o pouco que é específico do PostgreSQL (índice `pg_trgm`, *trigger* do ADR-005, `REVOKE`); o restante é SQL comum, compatível com H2 em modo PostgreSQL. `ddl-auto=validate`.
- **P3 — mapeamento híbrido:** separado em Agenda, Prontuário e Segurança & Auditoria; JPA no domínio em Cadastros e Prescrição. Nos módulos separados, a regra ArchUnit também proíbe `jakarta.persistence` em `..domain..`.
- **Desempenho (RNF03):** busca de paciente com índice trigram em `nome` e índice único em `cpf_ativo`; toda listagem é paginada (`Pageable`, `size` ≤ 50); listagens usam projeções; `spring.jpa.open-in-view=false` para que nenhuma consulta escape da camada de aplicação.

## Consequências

- (+) Extrair a Agenda no PF é mover o schema `agenda` e trocar os adaptadores (QS-11).
- (+) Schema revisável no PR e reproduzível em qualquer ambiente.
- (+) O domínio da Agenda e do Prontuário é testável sem Hibernate (QS-13).
- (−) Sem FK entre módulos: um bug pode gravar `atendimento_id` inexistente. Mitigação: validação na porta e teste de integração; relatório periódico de órfãos.
- (−) Duas convenções de mapeamento. Mitigação: este ADR e as regras ArchUnit por módulo.
- (−) Relatórios não podem fazer JOIN livre entre schemas. Isso é intencional, e é o que a porta `ConsultaEstatisticaQuery` resolve.

## Verificação

- `ddl-auto=validate` no teste falha se entidades e migrações divergirem.
- Teste de arquitetura por módulo (domínio sem `jakarta.persistence` em agenda, prontuario e seguranca).
- QS-06: `EXPLAIN ANALYZE` da busca por nome usa o índice trigram.
- QS-08: contagem de SQL por requisição constante no histórico (sem N+1).
