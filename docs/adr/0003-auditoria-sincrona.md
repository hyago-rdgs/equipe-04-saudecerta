# ADR 0003 — Auditoria síncrona, no mesmo banco, em transação própria

- **Situação:** aceita (candidata a substituição na missão PF-2)
- **Data:** 2026-11-10

## Contexto

RNF02: todo acesso e alteração de prontuário deve ficar registrado (quem, quando, o quê). A leitura também conta.

## Opções consideradas

1. Síncrona, na mesma transação da operação.
2. Síncrona, em transação própria (`REQUIRES_NEW`).
3. Assíncrona, por eventos (fila ou publicação em memória).

## Decisão

Opção 2. Os módulos chamam `Auditoria.registrar(...)`; o `AuditoriaService` grava em transação separada.

## Custo e benefício

| | Mesma transação | Transação própria | Assíncrona |
|---|---|---|---|
| Benefício principal | tudo ou nada | tentativa fica registrada mesmo com rollback | não atrasa a resposta |
| Custo | rollback apaga a trilha da tentativa | uma escrita e uma conexão a mais por operação | infraestrutura de eventos; ordem e entrega |
| Risco | perder evidência de tentativas | registrar algo que depois não se concretizou | perder eventos se o processo cair |

## Consequências

- A trilha pode conter uma ação cujo efeito foi desfeito por rollback; por isso o texto registra a intenção.
- Cada operação auditada paga a latência de uma escrita extra (visível no Raio-X).
- Sob muita carga, o segundo uso de conexão por requisição pressiona o pool.
