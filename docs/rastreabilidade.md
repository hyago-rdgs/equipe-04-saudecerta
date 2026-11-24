# Matriz de rastreabilidade

Requisito → decisão → componente/tática → como se verifica. Ao concluir uma missão, acrescente as linhas correspondentes.

## Requisitos funcionais

| ID | Requisito | Componentes | Verificação |
|---|---|---|---|
| RF01 | Entrar com usuário e senha | `AuthController`, `AutenticacaoService` | `FluxoDaClinicaTest.recusaSenhaErrada` |
| RF02 | Acesso conforme o perfil | `SecurityConfig` | `FluxoDaClinicaTest.prontuarioRestritoAProfissionais` |
| RF03 | Manter pacientes (CPF válido e único) | `PacienteController`, `PacienteService`, `Cpf` | `CpfTest`; roteiro, etapa 2 |
| RF04 | Manter profissionais e especialidades | `ProfissionalController`, `ProfissionalService` | roteiro, etapa 6 |
| RF05 | Ver a agenda do dia por profissional | `AgendaController`, `AgendaService` | roteiro, etapa 3 |
| RF06 | Agendar consulta sem conflito de horário | `Consulta.agendar`, `Consulta.conflitaCom`, `AgendaService.agendar` | `ConsultaTest`; `AgendaServiceTest`; cenários concorrentes em `FluxoDaClinicaTest` (ADR 0011) |
| RF07 | Confirmar a chegada do paciente | `Consulta.confirmarChegada` | `ConsultaTest.cicloDeVida` |
| RF08 | Cancelar consulta com motivo | `Consulta.cancelar` | `ConsultaTest.regrasDeCancelamento` |
| RF09 | Resumo do dia | `PainelService` | roteiro, etapa 1 |
| RF10 | Iniciar atendimento (só o profissional da consulta, após a chegada) | `ProntuarioService.iniciar`, `Consulta.iniciarAtendimento` | `ConsultaTest.atendimentoExigeChegada` |
| RF11 | Registrar queixa, evolução e conduta | `Atendimento.registrar` | `AtendimentoTest` |
| RF12 | Finalizar atendimento, que passa a ser imutável | `Atendimento.finalizar` | `AtendimentoTest.finalizadoEhImutavel` |
| RF13 | Consultar o histórico clínico do paciente | `ProntuarioService.doPaciente` | `FluxoDaClinicaTest.prontuarioRestritoAProfissionais` |
| RF14 | Consultar a trilha de auditoria | `AuditoriaController`, `AuditoriaService` | `FluxoDaClinicaTest.leituraDeProntuarioEhAuditada` |
| RF15 | Remarcar consulta | `AgendaService.remarcar`, `Consulta.remarcar` | `ConsultaTest`, `FluxoDaClinicaTest.remarcacaoExibeVagasSemConflitosERegistraOsDoisHorarios` |
| RF16 | Prescrever medicamentos | `ProntuarioService.prescrever`, `Prescricao` | `PrescricaoTest`, `FluxoDaClinicaTest.prescricoesValidamResponsavelEAlergiaEConstamNoHistoricoEAuditoria` |
| RF17 | Relatórios de consultas | `PainelService.relatorioDeConsultas` | `PeriodoDoRelatorioTest`, `FluxoDaClinicaTest.relatorioContaConsultasNoPeriodoInclusivo` |
| RF18 | Portal do paciente | `PortalController`, `AgendaService.minhasConsultas` | `FluxoDaClinicaTest.portalUsaPacienteDoTokenESoCancelaConsultaFuturaPropria` |

## Requisitos não funcionais

| ID | Requisito | Decisão | Componente / tática | Verificação |
|---|---|---|---|---|
| RNF01 | Dados de saúde só para quem precisa (LGPD) | ADR 0004 | RBAC por rota; administrador não lê prontuário | `prontuarioRestritoAProfissionais` |
| RNF02 | Todo acesso ao prontuário é auditável | ADR 0003 | `Auditoria.registrar` em leitura e escrita; transação própria | `leituraDeProntuarioEhAuditada` |
| RNF03 | Senhas nunca em claro | ADR 0005 | hash BCrypt; token assinado | revisão de `Usuario` e `SecurityConfig` |
| RNF04 | Novos módulos sem impacto no núcleo | ADR 0001 | módulos com pacote `api`; sem ciclos | `ArquiteturaTest` (regras de módulo) |
| RNF05 | Regras de negócio testáveis sem infraestrutura | ADR 0002 | domínio puro; portas e adaptadores | `ArquiteturaTest.dominio_nao_conhece_framework...`; testes de domínio e dos casos de uso em memória (`AgendaServiceTest`, `ProntuarioServiceTest`) |
| RNF06 | Busca de paciente e abertura de prontuário em menos de 2 s | — | consultas simples; junção de nomes em lote (`AgendaService.detalhar`) | Raio-X (medição manual). **Lacuna:** não há teste automatizado de desempenho |
| RNF07 | Agenda disponível no horário da clínica | — | **Lacuna:** processo único, banco em memória | discutir em aula; relacionar com PF-1 |
| RNF08 | Recepção opera com poucos cliques | — | agendar a partir da própria grade | roteiro, etapa 3 |

As lacunas estão registradas de propósito: uma matriz honesta mostra também o que ainda não é atendido.
