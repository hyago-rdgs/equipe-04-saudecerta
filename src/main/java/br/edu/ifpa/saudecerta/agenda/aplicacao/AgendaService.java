package br.edu.ifpa.saudecerta.agenda.aplicacao;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifpa.saudecerta.agenda.api.AgendaFachada;
import br.edu.ifpa.saudecerta.agenda.api.ConsultaInfo;
import br.edu.ifpa.saudecerta.agenda.api.TotalDeConsultas;
import br.edu.ifpa.saudecerta.agenda.dominio.Consulta;
import br.edu.ifpa.saudecerta.auditoria.api.Auditoria;
import br.edu.ifpa.saudecerta.auditoria.api.EventoDeAuditoria;
import br.edu.ifpa.saudecerta.cadastro.api.CadastroFachada;
import br.edu.ifpa.saudecerta.cadastro.api.PacienteResumo;
import br.edu.ifpa.saudecerta.cadastro.api.ProfissionalResumo;
import br.edu.ifpa.saudecerta.compartilhado.erro.AcessoNegadoException;
import br.edu.ifpa.saudecerta.compartilhado.erro.ConflitoException;
import br.edu.ifpa.saudecerta.compartilhado.erro.RecursoNaoEncontradoException;
import br.edu.ifpa.saudecerta.compartilhado.erro.RegraDeNegocioException;
import br.edu.ifpa.saudecerta.seguranca.api.Perfil;
import br.edu.ifpa.saudecerta.seguranca.api.UsuarioLogado;

/**
 * Casos de uso da agenda. Esta camada ORQUESTRA: busca dados, chama o dominio
 * para decidir, salva e audita. As regras em si estao em Consulta.
 */
@Service
@Transactional
public class AgendaService implements AgendaFachada {

    private final ConsultaRepositorio consultas;
    private final CadastroFachada cadastro;
    private final Auditoria auditoria;
    private final Clock relogio;

    public AgendaService(ConsultaRepositorio consultas, CadastroFachada cadastro, Auditoria auditoria,
            Clock relogio) {
        this.consultas = consultas;
        this.cadastro = cadastro;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ConsultaInfo agendar(NovaConsulta pedido, UsuarioLogado quem) {
        cadastro.bloquearParaReserva(pedido.profissionalId(), pedido.pacienteId());
        PacienteResumo paciente = cadastro.paciente(pedido.pacienteId()).filter(PacienteResumo::ativo)
                .orElseThrow(() -> new RegraDeNegocioException("Paciente não encontrado ou inativo."));
        ProfissionalResumo profissional = cadastro.profissional(pedido.profissionalId())
                .filter(ProfissionalResumo::ativo)
                .orElseThrow(() -> new RegraDeNegocioException("Profissional não encontrado ou inativo."));

        Consulta nova = Consulta.agendar(paciente.id(), profissional.id(), pedido.inicio(), pedido.observacao(),
                LocalDateTime.now(relogio));

        verificarConflitos(nova, profissional.nome(), paciente.nome());

        Consulta salva = consultas.salvar(nova);
        auditar(quem, "CONSULTA_AGENDADA", salva, "Consulta com " + profissional.nome() + " em " + salva.getInicio());
        return detalhar(List.of(salva)).get(0);
    }

    public ConsultaInfo confirmarChegada(Long id, UsuarioLogado quem) {
        Consulta consulta = obterParaAlterar(id);
        consulta.confirmarChegada();
        Consulta salva = consultas.salvar(consulta);
        auditar(quem, "CHEGADA_CONFIRMADA", salva, "Paciente aguardando atendimento");
        return detalhar(List.of(salva)).get(0);
    }

    public ConsultaInfo cancelar(Long id, String motivo, UsuarioLogado quem) {
        Consulta consulta = obterParaAlterar(id);
        consulta.cancelar(motivo);
        Consulta salva = consultas.salvar(consulta);
        auditar(quem, "CONSULTA_CANCELADA", salva, "Motivo: " + salva.getMotivoCancelamento());
        return detalhar(List.of(salva)).get(0);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ConsultaInfo remarcar(Long id, LocalDateTime novoInicio, UsuarioLogado quem) {
        Consulta consulta = obterParaAlterar(id);
        cadastro.bloquearParaReserva(consulta.getProfissionalId(), consulta.getPacienteId());
        LocalDateTime inicioAnterior = consulta.getInicio();
        consulta.remarcar(novoInicio, LocalDateTime.now(relogio));
        verificarConflitos(consulta, "O profissional", "O paciente");
        Consulta salva = consultas.salvar(consulta);
        auditar(quem, "CONSULTA_REMARCADA", salva, "Horário anterior: " + inicioAnterior + " | Novo horário: " + novoInicio);
        return detalhar(List.of(salva)).get(0);
    }

    @Transactional(readOnly = true)
    public List<LocalTime> horariosLivres(Long profissionalId, LocalDate data, Long consultaId, UsuarioLogado quem) {
        Consulta atual = null;
        if (consultaId != null) {
            if (!quem.tem(Perfil.RECEPCAO) && !quem.tem(Perfil.ADMIN)) {
                throw new AcessoNegadoException("Somente a recepção ou administração pode remarcar consultas.");
            }
            atual = obter(consultaId);
            if (!atual.getProfissionalId().equals(profissionalId)) {
                throw new RegraDeNegocioException("Informe o profissional da consulta para remarcar.");
            }
        }
        if (data.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return List.of();
        }
        List<Consulta> ocupados = new ArrayList<>(consultas.doProfissionalNoDia(profissionalId, data));
        if (atual != null) {
            ocupados.addAll(consultas.doPacienteNoDia(atual.getPacienteId(), data));
            Long idAtual = atual.getId();
            ocupados.removeIf(c -> idAtual.equals(c.getId()));
        }
        LocalDateTime agora = LocalDateTime.now(relogio);
        int duracao = atual == null ? Consulta.DURACAO_MINUTOS : atual.getDuracaoMinutos();
        List<LocalTime> livres = new ArrayList<>();
        for (LocalTime h = Consulta.ABERTURA; h.isBefore(Consulta.FECHAMENTO)
                && !h.plusMinutes(duracao).isAfter(Consulta.FECHAMENTO); h = h.plusMinutes(Consulta.DURACAO_MINUTOS)) {
            LocalDateTime inicio = data.atTime(h);
            if (inicio.isAfter(agora) && (atual == null || !inicio.equals(atual.getInicio()))
                    && ocupados.stream().noneMatch(c -> c.ocupaIntervalo(inicio, inicio.plusMinutes(duracao)))) {
                livres.add(h);
            }
        }
        return livres;
    }

    @Transactional(readOnly = true)
    public List<ConsultaDoPaciente> minhasConsultas(UsuarioLogado quem) {
        List<Consulta> lista = consultas.doPaciente(pacienteDoPortal(quem));
        Map<Long, ProfissionalResumo> profissionais = cadastro
                .profissionais(lista.stream().map(Consulta::getProfissionalId).collect(Collectors.toSet()));
        LocalDateTime agora = LocalDateTime.now(relogio);
        return lista.stream()
                .map(c -> consultaDoPortal(c, profissionais.get(c.getProfissionalId()), agora))
                .toList();
    }

    public ConsultaDoPaciente cancelarPeloPortal(Long id, String motivo, UsuarioLogado quem) {
        Long pacienteId = pacienteDoPortal(quem);
        Consulta consulta = consultas.buscarDoPacienteParaAlterar(id, pacienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Consulta não encontrada."));
        LocalDateTime agora = LocalDateTime.now(relogio);
        consulta.cancelarPeloPaciente(motivo, agora);
        Consulta salva = consultas.salvar(consulta);
        auditar(quem, "CONSULTA_CANCELADA", salva, "Cancelada no portal. Motivo: " + salva.getMotivoCancelamento());
        return consultaDoPortal(salva, cadastro.profissional(salva.getProfissionalId()).orElse(null), agora);
    }

    // ---- AgendaFachada (uso por outros modulos) ----

    @Override
    public ConsultaInfo consulta(Long id) {
        return detalhar(List.of(obter(id))).get(0);
    }

    @Override
    public List<ConsultaInfo> consultasDoDia(LocalDate data, Long profissionalId) {
        List<Consulta> doDia = profissionalId == null
                ? consultas.doDia(data)
                : consultas.doProfissionalNoDia(profissionalId, data);
        return detalhar(doDia);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TotalDeConsultas> totaisPorProfissionalESituacao(LocalDate de, LocalDate ate) {
        return consultas.totaisPorProfissionalESituacao(de, ate);
    }

    @Override
    public void iniciarAtendimento(Long consultaId) {
        Consulta consulta = obterParaAlterar(consultaId);
        consulta.iniciarAtendimento();
        consultas.salvar(consulta);
    }

    @Override
    public void concluir(Long consultaId) {
        Consulta consulta = obterParaAlterar(consultaId);
        consulta.concluir();
        consultas.salvar(consulta);
    }

    // ---- apoio ----

    private Consulta obter(Long id) {
        return consultas.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Consulta " + id + " não encontrada."));
    }

    private Consulta obterParaAlterar(Long id) {
        return consultas.buscarParaAlterar(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Consulta " + id + " não encontrada."));
    }

    private void verificarConflitos(Consulta consulta, String profissional, String paciente) {
        LocalDate dia = consulta.getInicio().toLocalDate();
        if (temConflito(consulta, consultas.doProfissionalNoDia(consulta.getProfissionalId(), dia))) {
            throw new ConflitoException(profissional + " já tem consulta nesse horário.");
        }
        if (temConflito(consulta, consultas.doPacienteNoDia(consulta.getPacienteId(), dia))) {
            throw new ConflitoException(paciente + " já tem outra consulta nesse horário.");
        }
    }

    private static boolean temConflito(Consulta consulta, List<Consulta> candidatas) {
        return candidatas.stream().filter(c -> !Objects.equals(c.getId(), consulta.getId()))
                .anyMatch(consulta::conflitaCom);
    }

    private static Long pacienteDoPortal(UsuarioLogado quem) {
        if (!quem.tem(Perfil.PACIENTE) || quem.pacienteId() == null) {
            throw new AcessoNegadoException("O usuário não está vinculado a um paciente.");
        }
        return quem.pacienteId();
    }

    private static ConsultaDoPaciente consultaDoPortal(Consulta consulta, ProfissionalResumo profissional,
            LocalDateTime agora) {
        boolean futura = consulta.futura(agora);
        return new ConsultaDoPaciente(consulta.getId(), consulta.getInicio(), consulta.getDuracaoMinutos(),
                consulta.getStatus().name(), profissional == null ? "(profissional removido)" : profissional.nome(),
                profissional == null ? null : profissional.especialidade(), futura,
                futura && consulta.podeRemarcarOuCancelar());
    }

    /** Junta os nomes vindos do cadastro em duas consultas ao todo (e nao duas por consulta). */
    private List<ConsultaInfo> detalhar(List<Consulta> lista) {
        Map<Long, PacienteResumo> pacientes = cadastro
                .pacientes(lista.stream().map(Consulta::getPacienteId).collect(Collectors.toSet()));
        Map<Long, ProfissionalResumo> profissionais = cadastro
                .profissionais(lista.stream().map(Consulta::getProfissionalId).collect(Collectors.toSet()));
        return lista.stream()
                .sorted(Comparator.comparing(Consulta::getInicio))
                .map(c -> {
                    PacienteResumo p = pacientes.get(c.getPacienteId());
                    ProfissionalResumo m = profissionais.get(c.getProfissionalId());
                    return new ConsultaInfo(c.getId(), c.getInicio(), c.getDuracaoMinutos(), c.getStatus().name(),
                            c.getPacienteId(), p == null ? "(paciente removido)" : p.nome(),
                            p == null ? null : p.alertas(), c.getProfissionalId(),
                            m == null ? "(profissional removido)" : m.nome(),
                            m == null ? null : m.especialidade(), c.getObservacao(), c.getMotivoCancelamento());
                })
                .toList();
    }

    private void auditar(UsuarioLogado quem, String acao, Consulta consulta, String detalhe) {
        auditoria.registrarNaTransacaoAtual(new EventoDeAuditoria(quem.login(), quem.perfil().name(), acao, "Consulta",
                consulta.getId(), consulta.getPacienteId(), detalhe));
    }
}
