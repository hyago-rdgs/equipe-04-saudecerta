package br.edu.ifpa.saudecerta.painel.aplicacao;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;

import br.edu.ifpa.saudecerta.agenda.api.AgendaFachada;
import br.edu.ifpa.saudecerta.agenda.api.ConsultaInfo;
import br.edu.ifpa.saudecerta.agenda.api.TotalDeConsultas;
import br.edu.ifpa.saudecerta.cadastro.api.CadastroFachada;
import br.edu.ifpa.saudecerta.cadastro.api.ProfissionalResumo;
import br.edu.ifpa.saudecerta.painel.dominio.PeriodoDoRelatorio;
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

    public RelatorioDeConsultas relatorioDeConsultas(LocalDate de, LocalDate ate) {
        var periodo = new PeriodoDoRelatorio(de, ate);
        List<TotalDeConsultas> contagens = agenda.totaisPorProfissionalESituacao(periodo.de(), periodo.ate());
        Map<Long, Long> totaisPorId = new TreeMap<>();
        Map<String, Long> porSituacao = new TreeMap<>();
        for (TotalDeConsultas contagem : contagens) {
            totaisPorId.merge(contagem.profissionalId(), contagem.total(), Long::sum);
            porSituacao.merge(contagem.situacao(), contagem.total(), Long::sum);
        }

        Map<Long, ProfissionalResumo> profissionais = cadastro.profissionais(totaisPorId.keySet());
        Map<String, Long> porEspecialidade = new TreeMap<>();
        var porProfissional = new ArrayList<RelatorioDeConsultas.TotalPorProfissional>();
        for (var entrada : totaisPorId.entrySet()) {
            Long id = entrada.getKey();
            long total = entrada.getValue();
            ProfissionalResumo profissional = profissionais.get(id);
            String nome = profissional == null ? "Profissional #" + id : profissional.nome();
            String especialidade = profissional == null || profissional.especialidade() == null
                    ? "Sem especialidade" : profissional.especialidade();
            porEspecialidade.merge(especialidade, total, Long::sum);
            porProfissional.add(new RelatorioDeConsultas.TotalPorProfissional(id, nome, total));
        }

        long total = porSituacao.values().stream().mapToLong(Long::longValue).sum();
        return new RelatorioDeConsultas(periodo.de(), periodo.ate(), total, porSituacao, porProfissional,
                porEspecialidade);
    }

    private static int contar(List<ConsultaInfo> consultas, String status) {
        return (int) consultas.stream().filter(c -> c.status().equals(status)).count();
    }
}
