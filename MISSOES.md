# Missões da equipe

O SaúdeCerta chega funcionando, mas incompleto. Cada missão abaixo é uma parte que falta. Os pontos de entrada já existem no código: procure por `MISSAO` ou por `MissaoPendenteException`.

O professor define quais missões são obrigatórias e os prazos. As missões **E3** pertencem ao protótipo (camadas + API REST); as **PF**, ao Projeto Final (serviços e reutilização).

Regras que valem para todas:

- `mvn test` precisa terminar verde, incluindo o `ArquiteturaTest`. Se uma regra de arquitetura atrapalhar, não a apague: discuta e, se for o caso, registre uma ADR justificando a mudança.
- Toda regra de negócio nova fica no **domínio** e ganha teste de domínio.
- Toda decisão que tenha alternativa real vira uma ADR em `docs/adr/` (use `0000-modelo.md`).
- Commits pequenos, com mensagem que diga o que mudou e por quê.

---

## Protótipo (E3)

### E3-1 — Remarcar consulta

A recepção muda o horário de uma consulta sem precisar cancelar e criar outra.

- Onde: `Consulta` (novo comportamento no domínio), `AgendaService.remarcar`, `PUT /api/consultas/{id}/horario` (já existe e responde 501), botão "Remarcar…" na tela da agenda.
- Aceite:
  - só consultas `AGENDADA` ou `CONFIRMADA` podem ser remarcadas;
  - o novo horário obedece às mesmas regras do agendamento (futuro, grade de 30 min, 07h–19h, não domingo) e não conflita com outra consulta do profissional nem do paciente;
  - a consulta remarcada volta para `AGENDADA`;
  - a operação fica na auditoria com o horário antigo e o novo;
  - há testes de domínio para cada regra e a tela permite escolher o novo horário entre os livres (`GET /api/agenda/horarios-livres`).
- Pergunta para a ADR: a `Consulta` hoje tem `inicio` imutável (`final`). Você torna o campo mutável ou cria uma nova consulta ligada à antiga? O que cada opção custa para a auditoria?

### E3-2 — Prescrições

O profissional emite a prescrição de medicamentos durante o atendimento.

- Onde: comece por `PrescricaoTest` — apague a linha `@Disabled`, rode os testes e implemente `Prescricao` até passarem. Depois crie a porta de saída, o adaptador JPA, o caso de uso e complete `GET` e `POST /api/atendimentos/{id}/prescricoes`. Na tela de atendimento, substitua o cartão da missão.
- Aceite:
  - os cinco testes de `PrescricaoTest` passam;
  - só o profissional responsável prescreve, e só em atendimento em aberto;
  - prescrição de medicamento citado nos alertas do paciente é recusada com mensagem clara (HTTP 422);
  - emitir e ler prescrição deixam registro na auditoria;
  - o histórico do paciente mostra as prescrições de cada atendimento.
- Opcional: solicitação de exames, com a mesma estrutura.

### E3-3 — Relatórios

A administração consulta totais de consultas por período.

- Onde: `PainelService.relatorioDeConsultas`, `GET /api/relatorios/consultas?de=&ate=` (responde 501), tela "Relatórios".
- Aceite:
  - totais por situação, por profissional e por especialidade no período informado;
  - período inválido (fim antes do início, ou maior que 1 ano) responde 400/422 com mensagem clara;
  - a `AgendaFachada` ganha o que for preciso — o módulo de relatórios não acessa a persistência da agenda diretamente (o `ArquiteturaTest` vai conferir).
- Pergunta para a ADR: agregar em memória no Java ou com `GROUP BY` no banco? Meça com o Raio-X antes de decidir.

### E3-4 — Portal do paciente

O paciente entra com o próprio usuário e vê as suas consultas.

- Onde: `GET /api/portal/consultas` (responde 501), tela "Minhas consultas". O token já traz `pacienteId` (`UsuarioLogado.pacienteId()`).
- Aceite:
  - o paciente vê apenas as próprias consultas, futuras e passadas;
  - pode cancelar uma consulta futura sua (com motivo);
  - **não existe** forma de ver consulta de outro paciente trocando um número na URL — escreva um teste de integração que prove isso;
  - nenhum dado clínico (evolução, conduta) aparece no portal.

### E3-5 — Conflito de horário sob concorrência

Hoje o `AgendaService` verifica o conflito e depois grava. Duas requisições simultâneas podem passar juntas pela verificação e marcar duas consultas no mesmo horário.

- Aceite:
  - um teste demonstra o problema (duas threads agendando o mesmo horário);
  - a correção garante a regra mesmo sob concorrência;
  - uma ADR compara pelo menos duas soluções (restrição única no banco, bloqueio pessimista, bloqueio otimista, fila) quanto a custo e benefício.

### E3-6 — Testes dos casos de uso sem banco

Os testes de domínio já existem; os casos de uso só são exercitados pelo teste de integração, que é lento.

- Aceite: `AgendaService` e `ProntuarioService` testados com implementações **em memória** das portas (`ConsultaRepositorio`, `AtendimentoRepositorio`) e um `Clock` fixo, sem subir o Spring. Compare o tempo com o do `FluxoDaClinicaTest` e explique a diferença usando o vocabulário da disciplina.

---

## Projeto Final (PF)

### PF-1 — Extrair um serviço

Transforme um módulo em aplicação separada, com banco próprio, falando com o monólito por REST.

- Candidatos: `auditoria` (mais simples: só recebe eventos) ou `agenda` (mais rico: o prontuário e o painel dependem dela).
- Caminho: as interfaces dos pacotes `api` (`Auditoria`, `AgendaFachada`) são o contrato. No monólito, troque a implementação local por um cliente HTTP da mesma interface — quem usa a interface não deve precisar mudar.
- Aceite:
  - dois processos rodando, cada um com seu banco;
  - o monólito continua passando em todos os testes;
  - está decidido e documentado o que acontece quando o serviço está fora do ar (timeout, nova tentativa, fila, falha rápida);
  - diagrama C4 de contêineres atualizado e ADR com custo/benefício da extração.

### PF-2 — Auditoria assíncrona por eventos

A auditoria hoje é síncrona (ADR 0003): cada operação espera a gravação da trilha.

- Aceite:
  - os módulos publicam eventos e a auditoria os consome fora do caminho da requisição;
  - medições antes e depois (Raio-X) no documento;
  - a ADR nova substitui a 0003 e discute o que se perde: e se o processo cair entre a operação e a gravação do evento?

### PF-3 — Componente reutilizável

Empacote algo do projeto para ser usado por outra aplicação.

- Candidatos: o Raio-X (`compartilhado.raiox`) como biblioteca/starter do Spring Boot; o tratamento de erros (`compartilhado.erro`); a validação de CPF.
- Aceite: módulo Maven separado, com versão; o SaúdeCerta passa a depender dele; um segundo projeto mínimo usa o mesmo componente; README do componente explica interface, dependências e versionamento.

### PF-4 — Documentação consolidada

- Aceite: C4 (contexto, contêineres, componentes) refletindo o sistema final; ADRs de todas as decisões tomadas pela equipe; `docs/rastreabilidade.md` atualizada com os requisitos novos; roteiro de demonstração para a defesa.
