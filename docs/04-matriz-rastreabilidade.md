# 4. Matriz de rastreabilidade — requisitos ↔ arquitetura

A matriz liga cada requisito da E1 à **decisão** que o atende, ao **componente** que o implementa e à **verificação** que prova que funciona. Ela é a ferramenta de manutenção da arquitetura: ao mudar um requisito, a linha mostra o que revisar; ao mudar um componente, a busca reversa mostra quais requisitos podem quebrar.

Legenda de verificação: **U** teste unitário de domínio · **I** teste de integração (Spring + banco) · **S** teste de segurança por perfil · **A** teste de arquitetura (ArchUnit/Modulith) · **C** teste de carga · **M** verificação manual/revisão.

## 4.1 Requisitos não funcionais (drivers)

| Requisito | Driver | Decisão | Componente(s) | Tática | Verificação |
|---|---|---|---|---|---|
| RNF01 Segurança/privacidade | DA1 | ADR-003, ADR-002 | Cadeia de filtros, `@PreAuthorize` nos serviços, `AccessDeniedHandler`, `AuditoriaListener` | RBAC no caso de uso; menor privilégio; registrar negação | S QS-01 (matriz perfil × caso de uso) |
| RNF02 Auditabilidade | DA2 | ADR-002, ADR-005 | `ProntuarioService` (emissor), `event_publication`, `AuditoriaListener`, `RegistroAuditoria` | Outbox; listener idempotente; hash encadeado; append-only | I QS-03 (queda e reprocessamento), U QS-04 (adulteração) |
| RNF03 Desempenho | DA4 | ADR-002, ADR-006 | `PacienteRepository` (índice trigram), `AtendimentoJpaAdapter` | Índice, paginação, projeção, sem N+1, auditoria fora do caminho crítico | C QS-06, QS-08 |
| RNF04 Disponibilidade | DA5 | ADR-001, ADR-003 | Contêiner da API, Actuator, Spring Session JDBC | Healthcheck, restart automático, estado fora do processo | M QS-09 (`docker kill`) |
| RNF05 Modificabilidade | DA3 | ADR-001, ADR-002, ADR-006 | Portas públicas, eventos de domínio, schemas por módulo | Encapsular, intermediário, restringir dependências | A `modulosRespeitamFronteiras`; M QS-10 (`git diff --stat`) |
| RNF06 Usabilidade | DA6 | ADR-003, ADR-004 | `AgendaController` (horários livres), `GlobalExceptionHandler` (409 + sugestões) | Endpoint dedicado; erro acionável; sessão de turno | M QS-12 (teste cronometrado) |
| RNF07 Credenciais | DA1 | ADR-003 | `UsuarioService`, `BCryptPasswordEncoder(12)` | Hash com sal; bloqueio por tentativas | U/I QS-07 |
| RNF08 LGPD | DA1 | ADR-002, ADR-003, ADR-005 | Nginx (TLS), `RegistroAuditoria` sem conteúdo clínico, `RelatorioService` | Minimização; HTTPS; sem dado clínico em log ou navegador | M checklist LGPD; I log sem campos clínicos |
| RNF09 Testabilidade | DA3 | ADR-001, ADR-006 | Pacotes `domain`, `java.time.Clock` | Domínio isolado; portas | A regra `dominioIsolado`; U suíte < 10 s |
| RNF10 Interoperabilidade | DA3 | ADR-001 | Controllers + springdoc-openapi | Contrato OpenAPI | M `/v3/api-docs` cobre 100% dos endpoints |

## 4.2 Requisitos funcionais

| Requisito | Módulo | Componente(s) | Endpoint | Decisão | Verificação |
|---|---|---|---|---|---|
| RF01 Cadastrar paciente | Cadastros | `PacienteController`, `CadastroService`, `Paciente`, `Cpf` | `POST/GET /api/pacientes` | ADR-006 | U CPF inválido; I CPF duplicado → 409 |
| RF02 Profissional + especialidades | Cadastros | `ProfissionalController`, `Profissional` | `POST /api/profissionais` | — | U sem especialidade → erro |
| RF03 Disponibilidade | Agenda | `AgendaController`, `Disponibilidade` | `PUT …/disponibilidade`, `GET …/horarios-livres` | — | U `gerarHorarios` |
| RF04 Marcar consulta | Agenda | `ConsultaController`, `AgendamentoService`, `Consulta.marcar` | `POST /api/consultas` | ADR-004 | I 201 + status AGENDADA |
| RF05 Remarcar/cancelar | Agenda | `Consulta.remarcar/cancelar`, `slot_ativo` | `PATCH /api/consultas/{id}` | ADR-004 | I cancelar libera o slot |
| RF06 Impedir conflito | Agenda | `PoliticaDeConflito`, `ux_consulta_slot_ativo` | `POST /api/consultas` | **ADR-004** | U conflito; I QS-05 concorrente |
| RF07 Registrar atendimento | Prontuário | `ProntuarioController`, `ProntuarioService`, `Atendimento` | `POST /api/consultas/{id}/atendimento` | ADR-005 | I consulta → REALIZADA na mesma transação |
| RF08 Prescrição | Prescrição | `PrescricaoService`, `Prescricao`, `ItemPrescricao` | `POST /api/atendimentos/{id}/prescricoes` | ADR-006 | U sem itens → erro |
| RF09 Solicitar exame | Prescrição | `SolicitacaoExame` | `POST /api/atendimentos/{id}/exames` | — | U transições de status |
| RF10 Histórico | Prontuário | `ProntuarioService.consultarHistorico` | `GET /api/pacientes/{id}/prontuario` | ADR-002, ADR-006 | I ordem cronológica; C QS-08 |
| RF11 Autenticar e perfis | Segurança | `AuthController`, `SecurityConfig`, Spring Session | `POST /api/auth/login` | **ADR-003** | S matriz de perfis |
| RF12 Auditar prontuário | Segurança & Auditoria | evento `ProntuarioAcessado`, `AuditoriaListener` | (efeito de RF10/RF07) | **ADR-002** | I QS-03 |
| RF13 Relatórios | Relatórios | `RelatorioService`, `ConsultaEstatisticaQuery` | `GET /api/relatorios/consultas` | ADR-006 | I totais conferem com dados de teste |
| RF14 Paciente vê os próprios dados | Agenda/Cadastros | verificação `@titular` | `GET /api/pacientes/{id}/consultas` | ADR-003 | S QS-02 |
| RF15 Paciente solicita consulta | Agenda | mesmo caso de uso de RF04 | `POST /api/consultas` | ADR-003, ADR-004 | S paciente só para si; I conflito |
| RF16 Gerenciar usuários | Segurança | `UsuarioService.bloquear` | `POST/PATCH /api/usuarios` | ADR-003 | I bloquear → próxima requisição 401 |
| RF17 Consultar trilha | Segurança & Auditoria | `AuditoriaController` | `GET /api/auditoria` | ADR-002 | I filtros por paciente/usuário/período |
| RF18 Adendo | Prontuário | `Atendimento.adicionarAdendo` | `POST /api/atendimentos/{id}/adendos` | **ADR-005** | U original intacto |

## 4.3 Regras de negócio

| Regra | Onde é garantida | Decisão | Verificação |
|---|---|---|---|
| RN01 CPF único ativo | `CadastroService` + índice único `cpf_ativo` | ADR-006 | I duplicado → 409; inativo + novo → ok |
| RN02 Sem conflito de horário | `PoliticaDeConflito` + `ux_consulta_slot_ativo` | ADR-004 | U + I QS-05 (**teste exigido na E3**) |
| RN03 Atendimento só se a data já ocorreu | `Consulta.realizar(clock)` | — | U com `Clock.fixed` |
| RN04 Prescrição/exame só com atendimento | Construtores + `AtendimentoApi` | ADR-006 | U/I atendimento inexistente → 422 |
| RN05 Prontuário somente-anexação | `Atendimento` imutável, API sem PUT/DELETE, `REVOKE` | **ADR-005** | A sem *setters*; I `UPDATE` negado; I `DELETE` → 405 (**teste exigido na E3**) |
| RN06 Paciente só o próprio; recepção sem clínico | `@PreAuthorize` + `@titular` | ADR-003 | S QS-01, QS-02 |
| RN07 Cancelar só AGENDADA | `Consulta.cancelar()` | — | U |
| RN08 Dentro da disponibilidade e no futuro | `Consulta.marcar()`, `Disponibilidade.contem()` | — | U |
| RN09 Estados finais | `Consulta` | — | U parametrizado (todas as transições) |
| RN10 Profissional inativo sem marcação | `PoliticaDeConflito` via `CadastroPort` | ADR-001 | U com *stub* |
| RN11 Correção por adendo | `Atendimento.adicionarAdendo()` | ADR-005 | U |
| RN12 Retenção de 20 anos; inativar | sem exclusão física; `Paciente.inativar()` | ADR-005 | M política de backup; I inativar mantém prontuário |

## 4.4 Visão reversa — decisão → requisitos

| ADR | Requisitos atendidos | Componentes afetados se a decisão mudar |
|---|---|---|
| ADR-001 Monólito modular hexagonal | RNF04, RNF05, RNF09, RNF10, RN10, RT02, RT05 | Estrutura de pacotes inteira; `ArquiteturaTest` |
| ADR-002 Auditoria por eventos | RF12, RF17, RNF01, RNF02, RNF03, RNF08 | `ProntuarioService`, `AuditoriaListener`, `event_publication`, `RegistroAuditoria` |
| ADR-003 Sessão + RBAC | RF11, RF14–RF16, RNF01, RNF04, RNF06, RNF07, RN06 | `SecurityConfig`, `UsuarioService`, todos os `@PreAuthorize` |
| ADR-004 Concorrência na marcação | RF04–RF06, RF15, RNF06, RN02 | `PoliticaDeConflito`, `ConsultaJpaAdapter`, migração `V3` |
| ADR-005 Somente-anexação | RF07, RF18, RNF02, RN05, RN11, RN12 | `Atendimento`, `Adendo`, `ProntuarioController`, permissões do banco |
| ADR-006 Persistência | RF01, RF13, RNF03, RNF05, RNF09, RN01, RN04 | Migrações Flyway, adaptadores `infra`, regras ArchUnit por módulo |

## 4.5 Cobertura

| Conjunto | Total | Com decisão ou componente | Com verificação definida |
|---|---|---|---|
| RF | 18 | 18 | 18 |
| RN | 12 | 12 | 12 |
| RNF | 10 | 10 | 10 |

Lacuna conhecida: RNF06 e RNF08 têm verificação parcialmente manual (teste de usabilidade e checklist LGPD). Essas verificações acontecem com o protótipo da E3 e serão registradas no PF.
