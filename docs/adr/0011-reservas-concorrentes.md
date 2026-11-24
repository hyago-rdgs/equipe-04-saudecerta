# ADR 0011 — Reservas concorrentes na agenda

- **Situação:** aceita
- **Data:** 2026-11-24
- **Equipe:** equipe 04

## Contexto

RF06, RF15 e E3-5 exigem impedir sobreposição de consultas que compartilham o profissional ou o paciente. Com a sequência original de verificar e gravar, duas transações podem ler uma agenda vazia e gravar no mesmo horário. Uma consulta cancelada libera seu intervalo. O protótipo tem H2 para demonstração e um perfil PostgreSQL.

## Opções consideradas

| Opção | Benefício | Custo e limite |
| --- | --- | --- |
| Bloqueio pessimista nas linhas do profissional e do paciente | Protege intervalos sobrepostos mesmo com a agenda vazia. Reutiliza as regras de domínio e funciona entre instâncias com o mesmo banco. | Requisições que compartilham um cadastro esperam, inclusive quando reservam horários diferentes. Novos fluxos de reserva precisam adquirir os mesmos bloqueios. |
| Restrições únicas para horários ocupados | O banco rejeita duplicatas mesmo em escritas diretas. A contenção fica restrita às chaves disputadas. | Precisa de duas chaves, considerar cancelamentos e traduzir violações. Verificar apenas o horário inicial deixa escapar sobreposições de intervalos com inícios diferentes. Materializar todos os intervalos ocupados exige uma estrutura adicional. |
| Bloqueio otimista em um agregado de agenda | Evita esperar pelo bloqueio em leituras. | Requer um agregado versionado para cada recurso e tratamento de falhas com nova tentativa. Operações sobre horários diferentes também podem precisar tentar novamente. |

## Decisão

Adotar bloqueio pessimista, com a validação de sobreposição que já existe no domínio. Agendar e remarcar solicitam à fachada do Cadastro o bloqueio do profissional e depois do paciente. O Cadastro usa JPA com `PESSIMISTIC_WRITE` e exige uma transação existente. As operações usam `READ_COMMITTED`. Quando a segunda transação entra na verificação, ela enxerga a reserva já confirmada e, se houver conflito, recebe HTTP 409.

O bloqueio dura até o fim da transação. Os eventos de sucesso da Agenda usam essa mesma transação. Assim, a gravação da consulta e a auditoria confirmam juntas, e a operação que detém os bloqueios consegue concluir com uma conexão, mesmo quando o pool tem conexões esperando pela reserva. Os outros módulos podem continuar registrando eventos em transações próprias.

A [ADR da etapa de projeto arquitetural](ADR-004-concorrencia-marcacao.md) considerava uma restrição única centrada no início da consulta do profissional. O protótipo também protege o paciente e preserva a regra de intervalos da Agenda.

## Verificação e consequências

O teste sem bloqueio usa duas threads e uma barreira após a leitura para reproduzir duas reservas. Os testes de integração disparam pedidos simultâneos por profissional, por paciente e entre remarcação e agendamento, e conferem os códigos HTTP e a quantidade persistida. Um teste com 12 requisições simultâneas sobre um horário também exercita o limite do pool de 10 conexões da configuração padrão.

O bloqueio protege os caminhos de reserva da aplicação. Escritas diretas no banco precisam respeitar o mesmo protocolo. Caso a espera por recursos compartilhados se torne relevante, medir a contenção antes de mudar a granularidade do bloqueio.
