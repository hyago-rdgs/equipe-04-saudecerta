# ADR 0007 — Agregação do relatório de consultas

- **Situação:** aceita
- **Data:** 2026-11-24
- **Equipe:** equipe 04

## Contexto

A missão E3-3 (RF17 em `docs/rastreabilidade.md`) exige totais por situação, profissional e especialidade. O Painel consulta a Agenda pela sua fachada. A Agenda armazena o identificador do profissional e a situação da consulta. O Cadastro mantém o nome e a especialidade do profissional.

## Opções consideradas

1. **Java:** buscar uma projeção com o profissional e a situação de cada consulta do período e contar as ocorrências em memória.
2. **Banco:** agrupar por profissional e situação com `GROUP BY` na consulta JPQL da Agenda e retornar uma contagem por combinação.

As duas opções usam as mesmas fachadas. O Painel busca os profissionais em lote no Cadastro e consolida as contagens nas três dimensões do relatório.

## Medições com o Raio-X

Executamos as duas versões com H2 em memória e Java 21. Em cada cenário, fizemos 15 requisições de aquecimento e medimos 30 requisições autenticadas ao mesmo endpoint. A tabela mostra a mediana de `duracaoMs` do rastro HTTP e do passo `ConsultaRepositorioJpa.totaisPorProfissionalESituacao`. As duas versões retornaram os mesmos totais.

| Cenário | Java, HTTP | Java, passo da Agenda | Banco, HTTP | Banco, passo da Agenda |
| --- | ---: | ---: | ---: | ---: |
| Base de demonstração, 21 consultas | 4,18 ms | 0,56 ms | 4,36 ms | 0,85 ms |
| Base sintética, 30.000 consultas | 6,93 ms | 3,89 ms | 3,80 ms | 0,73 ms |

A base sintética distribuiu as consultas ao longo de 2020 entre quatro profissionais e cinco situações. A consulta do relatório cobriu de 01/01/2020 a 31/12/2020, com as duas datas incluídas. Os tempos representam uma medição local em H2.

## Decisão

Usar `GROUP BY` no banco. A base de demonstração apresentou diferença inferior a 0,2 ms na mediana HTTP. Com 30 mil consultas, o banco devolveu 20 contagens e reduziu a mediana HTTP em cerca de 45%.

## Custo e benefício

| | Java | Banco |
| --- | --- | --- |
| Dados retornados da consulta | Uma linha por consulta | Uma linha por combinação de profissional e situação |
| Trabalho principal | A aplicação materializa e agrupa as linhas | O banco agrupa as linhas |
| Manutenção | O agrupamento é feito em código Java | O adaptador da Agenda mantém uma consulta JPQL agregada |

## Consequências

A Agenda expõe apenas as contagens pela `AgendaFachada`. O Painel usa a especialidade registrada atualmente no Cadastro, pois o agendamento conserva apenas o identificador do profissional. Acompanhar a consulta com dados de produção e medir novamente no PostgreSQL caso o volume ou a distribuição mudem.
