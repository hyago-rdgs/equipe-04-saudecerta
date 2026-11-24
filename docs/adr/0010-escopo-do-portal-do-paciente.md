# ADR 0010 — Escopo dos dados no portal do paciente

- **Situação:** aceita
- **Data:** 2026-11-24
- **Equipe:** equipe 04

## Contexto

O RF18 permite consultar e cancelar consultas próprias, usando o vínculo de paciente autenticado. O prontuário e a agenda interna possuem campos adicionais reservados aos seus públicos.

## Opções consideradas

1. Reutilizar a resposta da agenda interna e selecionar os campos na interface.
2. Filtrar por paciente no servidor e publicar uma resposta específica do portal.

## Decisão

Usar o identificador do paciente obtido do JWT. A listagem consulta esse paciente e o cancelamento busca pela combinação de consulta e paciente. O portal recebe apenas os campos de agendamento que exibe e os indicadores de ações disponíveis.

| | Reutilizar a agenda interna | Resposta específica do portal |
| --- | --- | --- |
| Campos | Mudanças no objeto interno podem chegar à API do portal | Os campos públicos ficam explícitos e cobertos por teste |
| Acesso a outra consulta | Exige validar a propriedade em cada chamada que busca por ID | A consulta ao banco já incorpora a condição de propriedade |
| Manutenção | Reutiliza o formato existente | Acrescenta um DTO e consultas do portal |

## Consequências

Consultar um identificador fora da conta autenticada produz a mesma resposta de recurso ausente. A alteração do motivo e da situação é registrada na auditoria. Os testes verificam a listagem, o cancelamento e os campos retornados.
