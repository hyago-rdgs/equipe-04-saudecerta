# ADR 0001 — Monólito modular em vez de microsserviços

- **Situação:** aceita
- **Data:** 2026-11-10

## Contexto

Clínica de médio porte, uma equipe pequena, um único local de implantação. O requisito RNF04 pede que novos módulos (ex.: faturamento) entrem sem mexer no núcleo. A ementa pede evolução para arquitetura baseada em serviços no Projeto Final.

## Opções consideradas

1. Monólito em camadas sem fronteiras internas.
2. Monólito modular: um processo, módulos com interface pública explícita.
3. Microsserviços desde o início.

## Decisão

Opção 2. Cada pacote de primeiro nível é um módulo; outros módulos só usam o pacote `api`; não há relacionamento JPA entre módulos; o `ArquiteturaTest` reprova violações.

## Custo e benefício

| | Monólito simples | Monólito modular | Microsserviços |
|---|---|---|---|
| Benefício principal | mais rápido de começar | fronteiras claras com custo operacional baixo | implantação e escala independentes |
| Custo de implementar | baixo | baixo a médio (fachadas, IDs em vez de objetos) | alto (rede, contratos, observabilidade) |
| Custo de operar | um processo | um processo | vários processos e bancos |
| Risco | vira "bola de lama" | disciplina depende dos testes de arquitetura | complexidade distribuída sem necessidade |

## Consequências

- Consultas que cruzam módulos juntam dados em memória (veja `AgendaService.detalhar`), não com `JOIN`.
- Sem chave estrangeira entre módulos, o banco não impede uma consulta apontando para paciente inexistente; a aplicação valida.
- Extrair um módulo como serviço (PF-1) vira trocar a implementação de uma interface `api`.
