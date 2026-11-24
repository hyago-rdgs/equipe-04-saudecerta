# E3-6 — Testes dos casos de uso sem banco

`AgendaServiceTest` e `ProntuarioServiceTest` instanciam os serviços diretamente e injetam adaptadores de teste em memória para as portas de saída `ConsultaRepositorio` e `AtendimentoRepositorio`. O prontuário usa a fachada real da agenda, também apoiada pelo repositório em memória. Cadastro, auditoria e prescrições usam implementações locais de seus contratos. O `Clock.fixed` deixa data e hora previsíveis em toda execução.

Os testes exercitam a orquestração da camada de aplicação. Eles verificam reservas, conflitos, remarcação, cancelamento, abertura e finalização de atendimento, emissão de prescrições e registros de auditoria. A suíte de integração verifica também os adaptadores de produção, a segurança HTTP e as transações que garantem a proteção contra duplicidade.

## Medição

Três execuções locais com Java 21, Maven 3.9.11 e Surefire, com uma JVM por classe. Os números são os tempos por classe que o Surefire registra. A inicialização do contexto do Spring ocorre dentro da classe de integração. Compilar e iniciar o Maven ficam fora da tabela.

| Classe | Execução 1 | Execução 2 | Execução 3 | Mediana |
| --- | ---: | ---: | ---: | ---: |
| `AgendaServiceTest` | 0,123 s | 0,088 s | 0,087 s | 0,088 s |
| `ProntuarioServiceTest` | 0,103 s | 0,104 s | 0,115 s | 0,104 s |
| `FluxoDaClinicaTest` | 8,059 s | 7,409 s | 7,802 s | 7,802 s |

Para repetir em um terminal na pasta do projeto:

```sh
mvn "-Dtest=AgendaServiceTest,ProntuarioServiceTest,FluxoDaClinicaTest" -DreuseForks=false test
```

## Interpretação

A **inversão de dependência** permite trocar o adaptador JPA pelo adaptador em memória, mantendo as mesmas portas. Os testes dos casos de uso executam a lógica da camada de aplicação e do domínio com chamadas diretas e acesso a coleções. O `FluxoDaClinicaTest` inicializa a **infraestrutura** e atravessa o adaptador web, Spring Security, serviços transacionais, JPA e H2, além de exercitar a concorrência em threads distintas. Inicialização, autenticação e operações de persistência aumentam seu tempo.

As classes têm cenários e quantidades de testes diferentes. Os tempos mostram o custo observado em cada nível de teste deste projeto. A garantia de bloqueio depende dos testes de integração com transações reais.

Use `mvn test` para executar a suíte completa. Os testes de arquitetura verificam as dependências entre camadas e módulos.
