package br.edu.ifpa.saudecerta.agenda.aplicacao;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifpa.saudecerta.agenda.api.AgendaFachada;
import br.edu.ifpa.saudecerta.agenda.api.ConsultaInfo;
import br.edu.ifpa.saudecerta.agenda.dominio.Consulta;
import br.edu.ifpa.saudecerta.auditoria.api.Auditoria;
import br.edu.ifpa.saudecerta.auditoria.api.EventoDeAuditoria;
import br.edu.ifpa.saudecerta.cadastro.api.CadastroFachada;
import br.edu.ifpa.saudecerta.cadastro.api.PacienteResumo;
import br.edu.ifpa.saudecerta.cadastro.api.ProfissionalResumo;
import br.edu.ifpa.saudecerta.compartilhado.erro.ConflitoException;
import br.edu.ifpa.saudecerta.compartilhado.erro.MissaoPendenteException;
import br.edu.ifpa.saudecerta.compartilhado.erro.RecursoNaoEncontradoException;
import br.edu.ifpa.saudecerta.compartilhado.erro.RegraDeNegocioException;
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

    public ConsultaInfo agendar(NovaConsulta pedido, UsuarioLogado quem) {
        PacienteResumo paciente = cadastro.paciente(pedido.pacienteId()).filter(PacienteResumo::ativo)
                .orElseThrow(() -> new RegraDeNegocioException("Paciente não encontrado ou inativo."));
        ProfissionalResumo profissional = cadastro.profissional(pedido.profissionalId())
                .filter(ProfissionalResumo::ativo)
                .orElseThrow(() -> new RegraDeNegocioException("Profissional não encontrado ou inativo."));

        Consulta nova = Consulta.agendar(paciente.id(), profissional.id(), pedido.inicio(), pedido.observacao(),
                LocalDateTime.now(relogio));

        LocalDate dia = nova.getInicio().toLocalDate();
        // Limitacao conhecida (missao E3-5): duas requisicoes simultaneas podem passar
        // juntas por esta verificacao. Como voce garantiria a regra no banco?
        if (consultas.doProfissionalNoDia(profissional.id(), dia).stream().anyMatch(nova::conflitaCom)) {
            throw new ConflitoException(profissional.nome() + " já tem consulta nesse horário.");
        }
        if (consultas.doPacienteNoDia(paciente.id(), dia).stream().anyMatch(nova::conflitaCom)) {
            throw new ConflitoException(paciente.nome() + " já tem outra consulta nesse horário.");
        }

        Consulta salva = consultas.salvar(nova);
        auditar(quem, "CONSULTA_AGENDADA", salva, "Consulta com " + profissional.nome() + " em " + salva.getInicio());
        return detalhar(List.of(salva)).get(0);
    }

    public ConsultaInfo confirmarChegada(Long id, UsuarioLogado quem) {
        Consulta consulta = obter(id);
        consulta.confirmarChegada();
        Consulta salva = consultas.salvar(consulta);
        auditar(quem, "CHEGADA_CONFIRMADA", salva, "Paciente aguardando atendimento");
        return detalhar(List.of(salva)).get(0);
    }

    public ConsultaInfo cancelar(Long id, String motivo, UsuarioLogado quem) {
        Consulta consulta = obter(id);
        consulta.cancelar(motivo);
        Consulta salva = consultas.salvar(consulta);
        auditar(quem, "CONSULTA_CANCELADA", salva, "Motivo: " + salva.getMotivoCancelamento());
        return detalhar(List.of(salva)).get(0);
    }

    /** MISSAO E3-1: remarcar uma consulta para outro horario. */
    public ConsultaInfo remarcar(Long id, LocalDateTime novoInicio, UsuarioLogado quem) {
        throw new MissaoPendenteException("E3-1",
                "Remarcação ainda não implementada. Veja a missão E3-1 em MISSOES.md.");
    }

    /** Horarios ainda vagos de um profissional em um dia (grade de 30 minutos). */
    public List<LocalTime> horariosLivres(Long profissionalId, LocalDate data) {
        Set<LocalTime> ocupados = consultas.doProfissionalNoDia(profissionalId, data).stream()
                .filter(Consulta::ocupaHorario)
                .map(c -> c.getInicio().toLocalTime())
                .collect(Collectors.toSet());
        LocalDateTime agora = LocalDateTime.now(relogio);
        List<LocalTime> livres = new ArrayList<>();
        for (LocalTime h = Consulta.ABERTURA; h.isBefore(Consulta.FECHAMENTO); h = h
                .plusMinutes(Consulta.DURACAO_MINUTOS)) {
            if (!ocupados.contains(h) && data.atTime(h).isAfter(agora)) {
                livres.add(h);
            }
        }
        return livres;
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
    public void iniciarAtendimento(Long consultaId) {
        Consulta consulta = obter(consultaId);
        consulta.iniciarAtendimento();
        consultas.salvar(consulta);
    }

    @Override
    public void concluir(Long consultaId) {
        Consulta consulta = obter(consultaId);
        consulta.concluir();
        consultas.salvar(consulta);
    }

    // ---- apoio ----

    private Consulta obter(Long id) {
        return consultas.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Consulta " + id + " não encontrada."));
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
        auditoria.registrar(new EventoDeAuditoria(quem.login(), quem.perfil().name(), acao, "Consulta",
                consulta.getId(), consulta.getPacienteId(), detalhe));
    }
}
