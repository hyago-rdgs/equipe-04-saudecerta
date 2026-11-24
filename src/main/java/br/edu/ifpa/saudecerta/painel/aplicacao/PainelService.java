package br.edu.ifpa.saudecerta.painel.aplicacao;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import br.edu.ifpa.saudecerta.agenda.api.AgendaFachada;
import br.edu.ifpa.saudecerta.agenda.api.ConsultaInfo;
import br.edu.ifpa.saudecerta.cadastro.api.CadastroFachada;
import br.edu.ifpa.saudecerta.compartilhado.erro.MissaoPendenteException;
import br.edu.ifpa.saudecerta.seguranca.api.Perfil;
import br.edu.ifpa.saudecerta.seguranca.api.UsuarioLogado;

/**
 * Resumo do dia. Nao tem dados proprios: so compoe o que agenda e cadastro
 * publicam em suas fachadas (um modulo "de leitura").
 */
@Service
public class PainelService {

    private final AgendaFachada agenda;
    private final CadastroFachada cadastro;
    private final Clock relogio;

    public PainelService(AgendaFachada agenda, CadastroFachada cadastro, Clock relogio) {
        this.agenda = agenda;
        this.cadastro = cadastro;
        this.relogio = relogio;
    }

    /** Profissionais veem apenas a propria agenda; recepcao e administracao veem a clinica toda. */
    public PainelDoDia hoje(UsuarioLogado quem) {
        LocalDate hoje = LocalDate.now(relogio);
        Long filtro = quem.tem(Perfil.PROFISSIONAL) ? quem.profissionalId() : null;
        List<ConsultaInfo> consultas = agenda.consultasDoDia(hoje, filtro);
        return new PainelDoDia(hoje, consultas.size(), contar(consultas, "AGENDADA"),
                contar(consultas, "CONFIRMADA"), contar(consultas, "EM_ATENDIMENTO"),
                contar(consultas, "REALIZADA"), contar(consultas, "CANCELADA"),
                cadastro.totalDePacientesAtivos(), cadastro.profissionaisAtivos().size(), consultas);
    }

    /** MISSAO E3-3: relatorios de consultas por periodo, profissional e especialidade. */
    public Object relatorioDeConsultas(LocalDate de, LocalDate ate) {
        throw new MissaoPendenteException("E3-3", "Relatórios ainda não implementados. Veja a missão E3-3 em MISSOES.md.");
    }

    private static int contar(List<ConsultaInfo> consultas, String status) {
        return (int) consultas.stream().filter(c -> c.status().equals(status)).count();
    }
}
