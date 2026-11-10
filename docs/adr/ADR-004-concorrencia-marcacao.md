# ADR-004 — Controle de concorrência na marcação de consultas

| Campo | Valor |
|---|---|
| Status | **Aceito** |
| Data | 10/11/2026 |
| Decisores | Equipe SaúdeCerta |
| Drivers | DA7 Integridade da agenda · DA6 Usabilidade · DA4 Desempenho |
| Requisitos | RN02, RF04, RF05, RF06, RF15 |

## Contexto

O RN02 proíbe duas consultas AGENDADAS do mesmo profissional no mesmo horário. A verificação ingênua, que consulta se o horário está livre e então insere, falha quando duas recepcionistas, ou uma recepcionista e um paciente pelo portal (RF15), marcam o mesmo horário ao mesmo tempo. As duas consultas veem o horário livre e as duas inserem. Esse é exatamente o problema que o sistema veio resolver (E1, 2.2).

Premissa: os horários são uma grade de duração fixa por disponibilidade (RF03 gera “horários marcáveis”). Logo, conflito é o mesmo profissional no mesmo início de horário.

## Alternativas consideradas

| # | Alternativa | Correta sob concorrência? | Prós | Contras |
|---|---|---|---|---|
| A | Verificação só na aplicação (*check-then-insert*) | **Não** | Simples | Condição de corrida |
| B | **Lock pessimista** (`SELECT ... FOR UPDATE` na linha do profissional antes de inserir) | Sim | Mensagem de erro controlada | Serializa todas as marcações do profissional; risco de deadlock se a ordem de bloqueio variar; depende de o desenvolvedor lembrar o lock em todo caminho |
| C | **Lock otimista** num agregado “agenda do dia” com `@Version` | Sim | Sem bloqueio | Cria um agregado artificial; conflitos entre horários *diferentes* do mesmo dia geram retry desnecessário |
| D | **Restrição única no banco** sobre (profissional, horário) das consultas ativas | Sim, garantida pelo SGBD | Funciona em todo caminho de escrita, inclusive scripts; custo zero na leitura | Precisa traduzir a violação para erro de negócio; índice parcial (`WHERE status='AGENDADA'`) não existe no H2 |
| E | Isolamento `SERIALIZABLE` | Sim | Sem código extra | Falhas de serialização também em transações não relacionadas; exige retry genérico; comportamento difere entre H2 e PostgreSQL |

## Análise custo/benefício

| Critério | Peso | A | B | C | **D + validação no domínio** | E |
|---|---|---|---|---|---|---|
| Correção sob concorrência | 5 | 1 | 5 | 5 | **5** | 5 |
| Mensagem clara ao usuário (RNF06) | 3 | 4 | 4 | 3 | **5** | 2 |
| Independe de disciplina do dev | 4 | 1 | 2 | 3 | **5** | 4 |
| Desempenho / contenção | 2 | 5 | 3 | 3 | **5** | 2 |
| Portabilidade H2 ↔ PostgreSQL | 2 | 5 | 4 | 5 | **5** (com a técnica abaixo) | 2 |
| **Total (máx. 80)** | | 41 | 59 | 62 | **80** | 55 |

## Decisão

**Duas camadas, cada uma com um papel:**

1. **Domínio (caminho comum, mensagem clara):** `PoliticaDeConflito.verificar()` consulta se o slot está ocupado e, se estiver, lança `ConflitoDeHorarioException`. Resolve 99,9% dos casos com uma mensagem boa e sugestões de horário.
2. **Banco (garantia final):** coluna derivada `slot_ativo`, igual a `data_hora` quando o status é AGENDADA e `NULL` nos demais, com índice único:

```sql
-- V3__agenda_conflito.sql (Flyway; funciona em H2 e PostgreSQL)
ALTER TABLE agenda.consulta ADD COLUMN slot_ativo TIMESTAMP NULL;
CREATE UNIQUE INDEX ux_consulta_slot_ativo ON agenda.consulta (profissional_id, slot_ativo);
-- NULLs não colidem num índice único: consultas CANCELADAS e REALIZADAS liberam o slot.
```

O adaptador JPA mantém `slot_ativo` coerente com o status. Se duas transações passarem juntas pela camada 1, a segunda falha no commit com `DataIntegrityViolationException`, que o adaptador traduz em `ConflitoDeHorarioException`. A resposta é **409 Conflict** com `horariosSugeridos` (ver `02-camadas-componentes-dominio.md`).

Efeitos colaterais desejados:

- **Cancelar** (RF05) zera `slot_ativo`: o horário volta a ficar livre sem lógica adicional.
- **Remarcar** atualiza `slot_ativo` na mesma transação, e a restrição protege o novo horário.

## Consequências

- (+) A regra vale para qualquer caminho de escrita: API, portal do paciente, scripts de migração.
- (+) Sem bloqueios: marcações em horários diferentes nunca se atrapalham.
- (+) Mesmo comportamento em H2 (dev/testes) e PostgreSQL (produção).
- (−) A regra existe em dois lugares. Mitigação: o teste QS-05 garante que ambos concordam.
- (−) Só detecta sobreposição exata de início, graças à premissa da grade fixa. **Gatilho de revisão:** se surgirem consultas de durações diferentes para o mesmo profissional, migrar para uma *exclusion constraint* do PostgreSQL (`EXCLUDE USING gist (profissional_id WITH =, periodo WITH &&)`) e abandonar o suporte a H2 nesse ponto.

## Verificação

- Teste unitário de `PoliticaDeConflito` (dois agendamentos no mesmo horário → exceção).
- QS-05: teste concorrente com 20 threads no mesmo slot contra PostgreSQL via Testcontainers → exatamente 1 sucesso e 19 respostas 409.
- Teste: cancelar e marcar de novo o mesmo slot → sucesso.
