# ADR 0009 — Armazenamento e leitura de prescrições

- **Situação:** aceita
- **Data:** 2026-11-24
- **Equipe:** equipe 04

## Contexto

O RF16 pede prescrições emitidas pelo responsável durante o atendimento aberto, com leitura no histórico, verificação dos alertas e registro na auditoria.

## Opções consideradas

1. Guardar cada prescrição e seus itens ordenados em tabelas próprias.
2. Anexar um texto de prescrição ao registro clínico do atendimento.

## Decisão

Usar tabelas próprias com vínculo pelo identificador do atendimento. O domínio valida os itens antes da emissão. Emissão e finalização bloqueiam alterações concorrentes no mesmo atendimento. O histórico carrega as prescrições em lote e a auditoria identifica a ação e o atendimento.

| | Prescrição estruturada | Texto no registro clínico |
| --- | --- | --- |
| Validação | Medicamento, dose, posologia e repetição são verificáveis por item | Exige interpretar o texto posteriormente |
| Auditoria e histórico | Cada emissão ganha identidade e data | A emissão fica misturada a revisões do texto |
| Persistência | Duas tabelas e uma consulta em lote | Usa uma coluna no atendimento |

## Consequências

Atendimentos anteriores aparecem com uma lista vazia até que haja prescrições. O bloqueio de alertas compara o nome informado com o texto normalizado do Cadastro. O modelo atual depende do nome registrado e não resolve sinônimos ou equivalências entre marcas e princípios ativos.
