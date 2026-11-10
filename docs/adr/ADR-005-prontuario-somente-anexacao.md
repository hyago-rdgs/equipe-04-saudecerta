# ADR-005 — Prontuário somente-anexação, com correção por adendo

| Campo | Valor |
|---|---|
| Status | **Aceito** |
| Data | 10/11/2026 |
| Decisores | Equipe SaúdeCerta |
| Drivers | DA2 Auditabilidade · DA1 Segurança · DA4 Desempenho |
| Requisitos | RN05, RN11, RN12, RF07, RF18, RNF02 |

## Contexto

O RN05 determina que registros de atendimento não sejam apagados nem sobrescritos, apenas complementados. Profissionais erram, porém: digitam a dose errada, esquecem uma informação. O sistema precisa permitir corrigir **sem perder o original** e registrar quem corrigiu e quando (RN11). O prontuário deve ser retido por no mínimo 20 anos (RN12, Lei 13.787/2018).

A persona Dr. Rafael (E1, 3.3) acrescenta que ele não quer perder o que digitou durante a consulta antes de salvar.

## Alternativas consideradas

| # | Alternativa | Atende RN05? | Prós | Contras |
|---|---|---|---|---|
| A | Edição livre + auditoria de alteração | **Não**: o original é sobrescrito; fica só um log de que mudou | Mais simples para o usuário | Viola a regra; reconstruir o original depende do log |
| B | **Versionamento** (Hibernate Envers: tabela `_AUD` com cada versão) | Parcial: guarda o histórico, mas o modelo continua editável | Pouco código | A versão “atual” sobrescreve a visível; `_AUD` também pode ser alterada; confunde “editar” com “anexar” |
| C | **Somente-anexação com adendos**: o atendimento é imutável e a correção é uma nova entidade `Adendo` ligada ao original | **Sim** | O histórico é o próprio modelo; o leitor vê original + adendos na ordem | O usuário não “edita”: precisa entender o adendo. A UI mostra o adendo junto do trecho original |
| D | **Event sourcing** do prontuário | Sim | Histórico completo por construção | Custo alto de aprendizado e de infraestrutura (projeções, reconstrução); desproporcional ao problema |

## Análise custo/benefício

| Critério | Peso | A | B | **C** | D |
|---|---|---|---|---|---|
| Conformidade com RN05/RN11 | 5 | 1 | 3 | **5** | 5 |
| Resistência a adulteração | 4 | 1 | 2 | **4** | 4 |
| Custo de implementação | 3 | 5 | 4 | **4** | 1 |
| Desempenho da leitura do histórico | 2 | 5 | 4 | **5** | 3 |
| Clareza para o usuário | 2 | 5 | 4 | **3** | 3 |
| **Total (máx. 80)** | | 44 | 51 | **69** | 56 |

## Decisão

**C**, com proteção em **três camadas**, para que uma falha numa delas não comprometa a regra:

| Camada | Mecanismo |
|---|---|
| **Domínio** | `Atendimento` e `Adendo` não têm *setters*; campos `final`; a única operação após criar é `adicionarAdendo(texto, autorId, clock)`. Na entidade JPA, `@org.hibernate.annotations.Immutable` faz o Hibernate ignorar alterações. |
| **API** | Não existem `PUT`, `PATCH` nem `DELETE` para atendimento, prescrição ou adendo. Só `POST`. |
| **Banco** | O papel de banco da aplicação tem só `SELECT, INSERT` em `prontuario.*` (`REVOKE UPDATE, DELETE`). Em PostgreSQL, um *trigger* `BEFORE UPDATE OR DELETE` rejeita a operação mesmo para outros papéis, exceto o de manutenção. |

**Rascunho:** para não perder o que está sendo digitado, o atendimento em andamento é salvo em `prontuario.rascunho_atendimento`, uma tabela editável, visível só ao autor, que é **apagada** quando o atendimento é finalizado. O rascunho não faz parte do prontuário. Os dados clínicos não ficam no `localStorage` do navegador (RNF08).

**Retenção (RN12):** não há exclusão física de prontuário. Inativar um paciente não afeta o prontuário. O backup segue a política de retenção da clínica.

## Consequências

- (+) Original e correções aparecem juntos, sem depender de log.
- (+) Junto com a trilha encadeada (ADR-002), dá para provar quem registrou o quê e quando.
- (+) Leitura simples: `SELECT` dos atendimentos e adendos, sem reconstruir versões.
- (−) A tabela só cresce. ~320 atendimentos/dia dá ~2,3 milhões de linhas em 20 anos, volume tranquilo para PostgreSQL com índice por `prontuario_id`.
- (−) Exige uma tela de adendo na UI, com explicação ao profissional.
- (−) A tabela de rascunho é exceção à regra e precisa de teste que garanta a remoção após finalizar.

## Verificação

- Teste unitário: a classe `Atendimento` não expõe métodos de alteração (ArchUnit: nenhum método `set*` em `..prontuario.domain..`).
- Teste de integração: `UPDATE prontuario.atendimento ...` com o usuário da aplicação → erro de permissão.
- Teste de API: `PUT/DELETE /api/atendimentos/{id}` → 405 Method Not Allowed.
- Teste: finalizar atendimento remove o rascunho.
