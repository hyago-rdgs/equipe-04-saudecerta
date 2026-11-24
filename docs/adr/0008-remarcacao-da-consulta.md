# ADR 0008 — Identidade da consulta na remarcação

- **Situação:** aceita
- **Data:** 2026-11-24
- **Equipe:** equipe 04

## Contexto

O RF15 permite mudar o horário de uma consulta agendada ou confirmada. A recepção precisa rastrear o horário anterior e o novo, e consultas canceladas continuam contando como tal nos relatórios.

## Opções consideradas

1. Atualizar o início da consulta e registrar os dois horários na auditoria.
2. Cancelar a consulta anterior e criar uma consulta vinculada com outro identificador.

## Decisão

Manter o identificador da consulta e tornar o início mutável por meio de `Consulta.remarcar`. A Agenda verifica a grade e conflitos de profissional e paciente. Uma gravação registra os dois horários em `CONSULTA_REMARCADA`, na mesma transação da mudança.

| | Atualizar a consulta | Criar outra consulta |
| --- | --- | --- |
| Vínculos | Mantém as referências existentes | Exige vínculo entre consultas e regra de navegação entre elas |
| Auditoria | A trilha conserva ambos os horários e quem fez a mudança | As duas consultas conservam seus horários. A trilha precisa ligar o cancelamento e a criação |
| Relatórios | A remarcação mantém uma única consulta | A contagem precisa distinguir remarcação de cancelamento |

## Consequências

O banco serializa mudanças feitas sobre a mesma consulta. A garantia contra reservas simultâneas em duas consultas distintas para um mesmo horário segue o bloqueio descrito na ADR 0011. O histórico de remarcações é consultado pela trilha de auditoria.
