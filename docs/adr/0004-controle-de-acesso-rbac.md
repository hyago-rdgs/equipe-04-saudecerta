# ADR 0004 — RBAC por rota, com regra de responsável no domínio

- **Situação:** aceita
- **Data:** 2026-11-10

## Contexto

RNF01: dados de saúde são sensíveis (LGPD). Há quatro papéis: administração, recepção, profissional e paciente.

## Opções consideradas

1. RBAC simples: o perfil decide o acesso a cada rota.
2. ABAC: decisão por atributos (quem é, de quem é o dado, vínculo de atendimento, horário).

## Decisão

Opção 1, com um complemento: "só o profissional responsável altera o atendimento" é regra de negócio e fica no domínio (`Atendimento.exigirResponsavel`). As regras por rota ficam todas em `SecurityConfig`. O administrador **não** lê prontuário.

## Custo e benefício

| | RBAC | ABAC |
|---|---|---|
| Benefício principal | simples de entender e auditar | privilégio mínimo de verdade |
| Custo | grosso: qualquer profissional lê qualquer prontuário | motor de políticas, mais testes, mais difícil de explicar |
| Risco | acesso além do necessário | regra errada bloqueia atendimento |

## Consequências

- A leitura ampla por profissionais é compensada pela auditoria de leitura (ADR 0003): não impede, mas deixa rastro.
- O portal do paciente (E3-4) exige uma checagem por dono do dado; RBAC sozinho não resolve.
