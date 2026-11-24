# ADR 0002 — Hexagonal na agenda e no prontuário; camadas simples no cadastro

- **Situação:** aceita
- **Data:** 2026-11-10

## Contexto

A agenda e o prontuário concentram as regras de negócio (conflito de horário, ciclo de vida da consulta, imutabilidade do atendimento). O cadastro é essencialmente CRUD. O requisito RNF05 pede regras testáveis sem infraestrutura.

## Opções consideradas

1. Camadas tradicionais em todos os módulos (entidade JPA = modelo).
2. Hexagonal em todos os módulos.
3. Hexagonal onde há regra de negócio; camadas onde é CRUD.

## Decisão

Opção 3. Em `agenda` e `prontuario`: domínio em Java puro, portas de saída como interfaces na camada de aplicação, adaptadores web e JPA. Em `cadastro`, `auditoria` e `seguranca`: web → aplicação → persistência, com entidades JPA.

## Custo e benefício

| | Camadas | Hexagonal |
|---|---|---|
| Benefício principal | pouco código, fácil de ler | domínio isolado, testes em milissegundos, troca de infraestrutura |
| Custo de implementar | baixo | classes extras: porta, adaptador, entidade JPA separada e mapeamento |
| Custo de manter | regra tende a vazar para service e entidade | todo campo novo passa pelo mapeamento |
| Risco | acoplamento ao framework | cerimônia sem retorno em CRUD |

## Consequências

- Dois estilos convivem; o nome dos pacotes indica qual é (`adaptador`/`dominio` × `modelo`/`persistencia`).
- Se o cadastro ganhar regras relevantes, esta decisão deve ser revista para ele.
- A camada de aplicação ainda usa anotações do Spring (`@Service`, `@Transactional`): pureza total foi considerada cara demais para o ganho.
