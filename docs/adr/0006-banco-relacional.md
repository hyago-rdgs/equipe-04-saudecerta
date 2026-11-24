# ADR 0006 — Banco relacional também para o prontuário

- **Situação:** aceita
- **Data:** 2026-11-10

## Contexto

O atendimento tem campos de texto livre (queixa, evolução, conduta) e tende a ganhar estruturas variáveis (prescrições, exames).

## Opções consideradas

1. Tudo em banco relacional.
2. Prontuário em banco de documentos; o restante em relacional.

## Decisão

Opção 1: H2 em memória no desenvolvimento e PostgreSQL pelo perfil `postgres`.

## Custo e benefício

| | Relacional | Documentos para o prontuário |
|---|---|---|
| Benefício principal | um banco, transações entre agenda e atendimento | esquema flexível por tipo de registro |
| Custo | esquema muda a cada estrutura nova | dois bancos para operar; sem transação única |
| Risco | tabelas genéricas demais com o tempo | inconsistência entre os bancos |

## Consequências

- Finalizar atendimento e concluir a consulta acontecem na mesma transação.
- O H2 em memória perde os dados ao reiniciar: bom para aula, inaceitável em produção.
- Como a persistência do prontuário está atrás de uma porta (`AtendimentoRepositorio`), trocar de banco afeta só o adaptador.
