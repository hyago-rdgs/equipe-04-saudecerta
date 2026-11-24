package br.edu.ifpa.saudecerta.prontuario.aplicacao;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifpa.saudecerta.agenda.api.AgendaFachada;
import br.edu.ifpa.saudecerta.agenda.api.ConsultaInfo;
import br.edu.ifpa.saudecerta.auditoria.api.Auditoria;
import br.edu.ifpa.saudecerta.auditoria.api.EventoDeAuditoria;
import br.edu.ifpa.saudecerta.cadastro.api.CadastroFachada;
import br.edu.ifpa.saudecerta.cadastro.api.PacienteResumo;
import br.edu.ifpa.saudecerta.cadastro.api.ProfissionalResumo;
import br.edu.ifpa.saudecerta.compartilhado.erro.AcessoNegadoException;
import br.edu.ifpa.saudecerta.compartilhado.erro.RecursoNaoEncontradoException;
import br.edu.ifpa.saudecerta.prontuario.dominio.Atendimento;
import br.edu.ifpa.saudecerta.seguranca.api.UsuarioLogado;

/** Casos de uso do prontuario. Todo acesso passa por aqui e e auditado. */
@Service
@Transactional
public class ProntuarioService {

    private final AtendimentoRepositorio atendimentos;
    private final AgendaFachada agenda;
    private final CadastroFachada cadastro;
    private final Auditoria auditoria;
    private final Clock relogio;

    public ProntuarioService(AtendimentoRepositorio atendimentos, AgendaFachada agenda, CadastroFachada cadastro,
            Auditoria auditoria, Clock relogio) {
        this.atendimentos = atendimentos;
        this.agenda = agenda;
        this.cadastro = cadastro;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    /**
     * Abre o atendimento de uma consulta. Se ja existir um atendimento para ela
     * (o profissional fechou a tela e voltou), devolve o existente.
     */
    public AtendimentoInfo iniciar(Long consultaId, UsuarioLogado quem) {
        ConsultaInfo consulta = agenda.consulta(consultaId);
        if (quem.profissionalId() == null || !quem.profissionalId().equals(consulta.profissionalId())) {
            throw new AcessoNegadoException("Esta consulta é de outro profissional.");
        }
        Atendimento existente = atendimentos.buscarPorConsulta(consultaId).orElse(null);
        if (existente != null) {
            auditar(quem, "PRONTUARIO_LIDO", existente, "Atendimento reaberto na tela");
            return detalhar(existente);
        }
        agenda.iniciarAtendimento(consultaId);
        Atendimento novo = atendimentos.salvar(Atendimento.iniciar(consultaId, consulta.pacienteId(),
                consulta.profissionalId(), LocalDateTime.now(relogio)));
        auditar(quem, "ATENDIMENTO_INICIADO", novo, "Atendimento da consulta " + consultaId);
        return detalhar(novo);
    }

    public AtendimentoInfo registrar(Long id, RegistroClinico registro, UsuarioLogado quem) {
        Atendimento atendimento = obter(id);
        atendimento.exigirResponsavel(quem.profissionalId());
        atendimento.registrar(registro.queixaPrincipal(), registro.evolucao(), registro.conduta());
        Atendimento salvo = atendimentos.salvar(atendimento);
        auditar(quem, "PRONTUARIO_ALTERADO", salvo, "Registro clínico salvo");
        return detalhar(salvo);
    }

    public AtendimentoInfo finalizar(Long id, UsuarioLogado quem) {
        Atendimento atendimento = obter(id);
        atendimento.exigirResponsavel(quem.profissionalId());
        atendimento.finalizar(LocalDateTime.now(relogio));
        Atendimento salvo = atendimentos.salvar(atendimento);
        agenda.concluir(salvo.getConsultaId());
        auditar(quem, "ATENDIMENTO_FINALIZADO", salvo, "Atendimento finalizado");
        return detalhar(salvo);
    }

    public AtendimentoInfo ler(Long id, UsuarioLogado quem) {
        Atendimento atendimento = obter(id);
        auditar(quem, "PRONTUARIO_LIDO", atendimento, "Leitura do atendimento " + id);
        return detalhar(atendimento);
    }

    /** Historico completo do paciente. A LEITURA tambem e auditada (requisito de privacidade). */
    public ProntuarioInfo doPaciente(Long pacienteId, UsuarioLogado quem) {
        PacienteResumo paciente = cadastro.paciente(pacienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paciente " + pacienteId + " não encontrado."));
        List<Atendimento> historico = atendimentos.doPaciente(pacienteId);
        Map<Long, ProfissionalResumo> profissionais = cadastro
                .profissionais(historico.stream().map(Atendimento::getProfissionalId).collect(Collectors.toSet()));
        auditoria.registrar(new EventoDeAuditoria(quem.login(), quem.perfil().name(), "PRONTUARIO_LIDO",
                "Prontuário", pacienteId, pacienteId, "Leitura do histórico completo"));
        return new ProntuarioInfo(paciente,
                historico.stream().map(a -> detalhar(a, profissionais.get(a.getProfissionalId()))).toList());
    }

    private Atendimento obter(Long id) {
        return atendimentos.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Atendimento " + id + " não encontrado."));
    }

    private AtendimentoInfo detalhar(Atendimento a) {
        return detalhar(a, cadastro.profissional(a.getProfissionalId()).orElse(null));
    }

    private static AtendimentoInfo detalhar(Atendimento a, ProfissionalResumo profissional) {
        return new AtendimentoInfo(a.getId(), a.getConsultaId(), a.getPacienteId(), a.getProfissionalId(),
                profissional == null ? "(profissional removido)" : profissional.nome(),
                profissional == null ? null : profissional.especialidade(), a.getInicio(), a.getFim(),
                a.getQueixaPrincipal(), a.getEvolucao(), a.getConduta(), a.getStatus().name());
    }

    private void auditar(UsuarioLogado quem, String acao, Atendimento atendimento, String detalhe) {
        auditoria.registrar(new EventoDeAuditoria(quem.login(), quem.perfil().name(), acao, "Atendimento",
                atendimento.getId(), atendimento.getPacienteId(), detalhe));
    }
}
