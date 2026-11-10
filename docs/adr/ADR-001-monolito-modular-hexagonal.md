# ADR-001 — Monólito modular com camadas hexagonais

| Campo | Valor |
|---|---|
| Status | **Aceito** |
| Data | 10/11/2026 (preliminar na E1, 20/10/2026) |
| Decisores | Equipe SaúdeCerta |
| Drivers | DA3 Modificabilidade · DA7 Integridade da agenda · DA1 Segurança · RT02 Equipe pequena · RT05 Extração de serviço no PF |
| Requisitos | RNF05, RNF09, RNF10, RN02 |

## Contexto

A clínica tem cerca de 20 profissionais, ~320 consultas por dia e no máximo ~40 usuários simultâneos (E1, 7.1). A equipe tem 3–4 pessoas e um semestre. Ao mesmo tempo, o sistema precisa receber módulos novos (faturamento, convênios) sem reescrever o núcleo (RNF05), e no Projeto Final pelo menos um módulo precisa virar serviço independente (RT05).

Escalar horizontalmente não é driver. Os drivers dominantes são segurança, auditabilidade, modificabilidade e consistência da agenda.

## Alternativas consideradas

| # | Alternativa | Prós | Contras |
|---|---|---|---|
| A | **Camadas tradicional** (pacotes `controller`/`service`/`repository` para o sistema inteiro) | Mais simples de começar; é o que a maioria dos tutoriais Spring mostra | Fatias técnicas, não de negócio: tudo depende de tudo. Extrair a Agenda no PF exigiria desembaraçar classes de todos os pacotes |
| B | **Monólito modular + hexagonal** (pacotes por módulo; cada um com `api/application/domain/infra`; fronteiras verificadas) | Um build, um deploy, um banco, transações locais. Fronteiras de módulo já correspondem aos serviços candidatos | Fronteiras dependem de verificação automatizada para não se degradarem. Todos os módulos escalam juntos |
| C | **Arquitetura baseada em serviços** (4–6 serviços de grão grosso, banco compartilhado) | Implantação independente; falha isolada por serviço | 4–6 pipelines e deploys; chamadas remotas na marcação (Agenda → Cadastros); transação distribuída para “registrar atendimento + marcar consulta REALIZADA” |
| D | **Microsserviços** (serviço por capacidade, banco por serviço, broker, gateway) | Máxima independência | Custo operacional incompatível com RT02; auditoria e consistência da agenda ficam distribuídas (sagas, consistência eventual) |

## Análise custo/benefício

**Benefício** — matriz ponderada da E1 (notas 1–5, peso pela prioridade dos drivers, máximo 140):

| Critério | Peso | A | **B** | C | D |
|---|---|---|---|---|---|
| DA1 Segurança | 5 | 3 | 4 | 4 | 3 |
| DA2 Auditabilidade | 5 | 3 | 4 | 4 | 2 |
| DA3 Modificabilidade | 4 | 2 | 4 | 5 | 5 |
| DA7 Integridade da agenda | 4 | 5 | 5 | 4 | 2 |
| DA4 Desempenho | 3 | 4 | 4 | 3 | 3 |
| DA5 Disponibilidade | 2 | 3 | 3 | 4 | 4 |
| RT02 Simplicidade | 5 | 5 | 4 | 2 | 1 |
| **Total** | | 101 | **114** | 103 | 75 |

**Custo** — estimativa da equipe para chegar ao protótipo da E3:

| Item | A | **B** | C | D |
|---|---|---|---|---|
| Unidades de implantação | 1 | **1** | 4–6 | 8+ |
| Infra adicional | — | **—** | gateway ou descoberta, N pipelines | broker, gateway, rastreamento distribuído, N bancos |
| Esforço extra de setup (pessoa-dia) | 0 | **≈ 1** (Spring Modulith, ArchUnit) | ≈ 6–8 | ≈ 15+ |
| Custo de extrair um serviço no PF | alto (reestruturar pacotes) | **baixo (trocar adaptadores)** | já extraído | já extraído |

B tem o melhor benefício por ponto de custo: custa cerca de um dia a mais que A e dá quase toda a modificabilidade de C. C fica em segundo lugar e é o destino planejado para o PF. A decisão adia o custo de C para quando ele traz retorno (a extração), em vez de pagá-lo desde o início.

## Decisão

Adotamos **B — monólito modular com camadas hexagonais dentro de cada módulo**:

- Seis módulos de nível superior em `br.edu.ifpa.saudecerta`: `cadastro`, `agenda`, `prontuario`, `prescricao`, `seguranca`, `relatorio`, mais `shared`.
- Cada módulo expõe só interfaces públicas no pacote raiz (`AgendaApi`, `CadastroApi`, `AtendimentoApi`) e eventos. O restante é interno.
- Comunicação entre módulos: chamada síncrona a porta pública quando a operação precisa ser atômica; evento de domínio quando o emissor não deve conhecer o receptor (auditoria, futuro faturamento).
- As fronteiras são **verificadas no build** com Spring Modulith (`ApplicationModules.verify()`) e ArchUnit.

## Consequências

**Positivas**

- (+) Transação local resolve RN02 e a atomicidade “atendimento + consulta REALIZADA” sem saga.
- (+) Segurança centralizada num único ponto de entrada (DA1).
- (+) Novo módulo entra como pacote que escuta eventos e usa portas (QS-10).
- (+) A Agenda pode ser extraída trocando adaptadores (QS-11).

**Negativas e mitigação**

- (−) Uma instância é ponto único de falha → healthcheck + restart automático; sessão no banco permite uma 2ª instância (ADR-003).
- (−) Um módulo com bug de memória derruba todos → limites de memória no contêiner e monitoramento pelo Actuator.
- (−) Tentação de “só desta vez” acessar a classe interna de outro módulo → o build falha.

## Verificação

- `ArquiteturaTest.modulosRespeitamFronteiras()` roda em todo build.
- Documentação gerada pelo Spring Modulith (`Documenter`) produz o diagrama de módulos a partir do código, e ele é comparado com o C4 componente.

## Quando revisar

- Se a clínica abrir novas unidades e o pico passar de ~200 usuários simultâneos.
- No PF, ao extrair a Agenda: este ADR será complementado por um ADR-007 (“Extração do Serviço de Agendamento”).
