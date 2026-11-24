package br.edu.ifpa.saudecerta.demo;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import br.edu.ifpa.saudecerta.agenda.aplicacao.ConsultaRepositorio;
import br.edu.ifpa.saudecerta.agenda.dominio.Consulta;
import br.edu.ifpa.saudecerta.agenda.dominio.StatusConsulta;
import br.edu.ifpa.saudecerta.cadastro.modelo.Especialidade;
import br.edu.ifpa.saudecerta.cadastro.modelo.Paciente;
import br.edu.ifpa.saudecerta.cadastro.modelo.Profissional;
import br.edu.ifpa.saudecerta.cadastro.persistencia.EspecialidadeRepository;
import br.edu.ifpa.saudecerta.cadastro.persistencia.PacienteRepository;
import br.edu.ifpa.saudecerta.cadastro.persistencia.ProfissionalRepository;
import br.edu.ifpa.saudecerta.prontuario.aplicacao.AtendimentoRepositorio;
import br.edu.ifpa.saudecerta.prontuario.dominio.Atendimento;
import br.edu.ifpa.saudecerta.prontuario.dominio.StatusAtendimento;
import br.edu.ifpa.saudecerta.seguranca.api.Perfil;
import br.edu.ifpa.saudecerta.seguranca.modelo.Usuario;
import br.edu.ifpa.saudecerta.seguranca.persistencia.UsuarioRepository;

/**
 * Carga inicial para a clinica ja abrir com movimento: cadastros, a agenda de
 * hoje e de amanha, historico clinico e um usuario de cada perfil.
 *
 * TODAS AS PESSOAS E REGISTROS AQUI SAO FICTICIOS.
 *
 * Este pacote e a unica excecao as regras de modulo: ele grava direto nos
 * repositorios para montar o cenario (inclusive consultas no passado, que a
 * regra de agendamento nao permitiria). Desligue com saudecerta.dados-demo=false.
 */
@Component
@ConditionalOnProperty(name = "saudecerta.dados-demo", havingValue = "true")
public class DadosDeDemonstracao implements CommandLineRunner {

    /** Senha de todos os usuarios de demonstracao. Troque antes de qualquer uso real. */
    public static final String SENHA_DEMO = "saude123";

    private final EspecialidadeRepository especialidades;
    private final ProfissionalRepository profissionais;
    private final PacienteRepository pacientes;
    private final UsuarioRepository usuarios;
    private final ConsultaRepositorio consultas;
    private final AtendimentoRepositorio atendimentos;
    private final PasswordEncoder codificador;
    private final Clock relogio;

    public DadosDeDemonstracao(EspecialidadeRepository especialidades, ProfissionalRepository profissionais,
            PacienteRepository pacientes, UsuarioRepository usuarios, ConsultaRepositorio consultas,
            AtendimentoRepositorio atendimentos, PasswordEncoder codificador, Clock relogio) {
        this.especialidades = especialidades;
        this.profissionais = profissionais;
        this.pacientes = pacientes;
        this.usuarios = usuarios;
        this.consultas = consultas;
        this.atendimentos = atendimentos;
        this.codificador = codificador;
        this.relogio = relogio;
    }

    @Override
    public void run(String... args) {
        if (usuarios.count() > 0) {
            return; // banco ja populado (ex.: perfil postgres)
        }

        Especialidade clinica = especialidades.save(new Especialidade("Clínica Geral"));
        Especialidade cardio = especialidades.save(new Especialidade("Cardiologia"));
        Especialidade pediatria = especialidades.save(new Especialidade("Pediatria"));
        Especialidade ortopedia = especialidades.save(new Especialidade("Ortopedia"));

        Profissional ana = profissionais.save(new Profissional("Dra. Ana Beatriz Moura", "CRM-PA 10482", clinica));
        Profissional rafael = profissionais.save(new Profissional("Dr. Rafael Tavares", "CRM-PA 11937", cardio));
        Profissional helena = profissionais.save(new Profissional("Dra. Helena Pimentel", "CRM-PA 12760", pediatria));
        Profissional caio = profissionais.save(new Profissional("Dr. Caio Nogueira", "CRM-PA 13215", ortopedia));

        Paciente joana = paciente("Joana Batista Ferreira", "15881399803", LocalDate.of(1988, 3, 14), "(91) 98811-2030", "Alergia a dipirona");
        Paciente miguel = paciente("Miguel Antônio Sarmento", "69879730917", LocalDate.of(1959, 11, 2), "(91) 99142-7781", "Hipertenso; usa losartana");
        Paciente larissa = paciente("Larissa Couto Maués", "11417075350", LocalDate.of(1996, 7, 25), "(91) 98470-1126", null);
        Paciente pedro = paciente("Pedro Henrique Lobato", "64556815177", LocalDate.of(2017, 1, 9), "(91) 99203-5540", "Asma; responsável: Carla Lobato");
        Paciente socorro = paciente("Maria do Socorro Pantoja", "88447294072", LocalDate.of(1947, 5, 30), "(91) 98765-4410", "Diabética; alergia a penicilina");
        Paciente thiago = paciente("Thiago Rezende Cardoso", "55761698757", LocalDate.of(1991, 9, 18), "(91) 99311-0874", null);
        Paciente beatriz = paciente("Beatriz Araújo Monteiro", "16897684600", LocalDate.of(2003, 12, 5), "(91) 98122-6693", null);
        Paciente raimundo = paciente("Raimundo Nonato Brito", "97880932054", LocalDate.of(1965, 2, 21), "(91) 99654-3008", "Marca-passo");
        Paciente camila = paciente("Camila Vasconcelos Dias", "81967273138", LocalDate.of(1979, 8, 11), "(91) 98533-9127", null);
        Paciente davi = paciente("Davi Lucas Miranda", "77848648842", LocalDate.of(2020, 4, 3), "(91) 99478-2265", "Responsável: Paula Miranda");

        String senha = codificador.encode(SENHA_DEMO);
        usuarios.save(new Usuario("admin", "Administração", senha, Perfil.ADMIN, null, null));
        usuarios.save(new Usuario("recepcao", "Lúcia (recepção)", senha, Perfil.RECEPCAO, null, null));
        usuarios.save(new Usuario("ana", ana.getNome(), senha, Perfil.PROFISSIONAL, ana.getId(), null));
        usuarios.save(new Usuario("rafael", rafael.getNome(), senha, Perfil.PROFISSIONAL, rafael.getId(), null));
        usuarios.save(new Usuario("joana", joana.getNome(), senha, Perfil.PACIENTE, null, joana.getId()));

        LocalDate hoje = LocalDate.now(relogio);

        // ---- Historico: atendimentos de dias anteriores, ja finalizados ----
        historico(joana, ana, hoje.minusDays(62), 9, 0, "Dor de cabeça recorrente há duas semanas",
                "Cefaleia tensional, sem sinais de alarme. PA 120x80. Exame neurológico normal.",
                "Orientada sobre sono e hidratação. Analgésico comum se dor (evitar dipirona - alergia). Retorno se piora.");
        historico(joana, ana, hoje.minusDays(20), 10, 30, "Retorno - cefaleia",
                "Melhora importante após ajuste de rotina de sono. Dois episódios leves no período.",
                "Manter orientações. Alta do acompanhamento; retorno se necessário.");
        historico(miguel, rafael, hoje.minusDays(35), 8, 30, "Controle de pressão arterial",
                "PA 150x95 em duas medidas. Refere esquecer a medicação à noite. Sem dor torácica ou dispneia.",
                "Reforçada adesão; solicitado MAPA e exames laboratoriais. Retorno em 30 dias com resultados.");
        historico(socorro, ana, hoje.minusDays(12), 14, 0, "Acompanhamento de diabetes",
                "Glicemia capilar 168 mg/dL em jejum. Sem lesões nos pés. Refere boa adesão à dieta.",
                "Mantida medicação. Solicitada hemoglobina glicada. Orientado autoexame dos pés.");
        historico(pedro, helena, hoje.minusDays(7), 11, 0, "Tosse e chiado no peito",
                "Sibilos esparsos bilaterais, sem esforço respiratório. Saturação 97%. Afebril.",
                "Crise leve de asma. Ajustado plano de ação com a responsável; retorno em 7 dias.");

        // ---- Agenda de hoje: um pouco de cada situacao ----
        Consulta c1 = consulta(larissa, ana, hoje, 8, 0, StatusConsulta.REALIZADA, "Check-up anual", null);
        atendimentos.salvar(Atendimento.reconstituir(null, c1.getId(), larissa.getId(), ana.getId(),
                hoje.atTime(8, 2), hoje.atTime(8, 27), "Check-up anual",
                "Assintomática. PA 110x70, FC 68. Exame físico sem alterações.",
                "Solicitados exames de rotina. Retorno com resultados.", StatusAtendimento.FINALIZADO));
        consulta(thiago, ana, hoje, 8, 30, StatusConsulta.CANCELADA, null, "Paciente avisou que não poderá vir");
        consulta(joana, ana, hoje, 9, 0, StatusConsulta.CONFIRMADA, "Retorno", null);
        consulta(socorro, ana, hoje, 9, 30, StatusConsulta.CONFIRMADA, "Trazer resultado da glicada", null);
        consulta(camila, ana, hoje, 10, 30, StatusConsulta.AGENDADA, "Primeira consulta", null);
        consulta(beatriz, ana, hoje, 15, 0, StatusConsulta.AGENDADA, null, null);

        consulta(miguel, rafael, hoje, 8, 30, StatusConsulta.CONFIRMADA, "Retorno com MAPA", null);
        consulta(raimundo, rafael, hoje, 9, 30, StatusConsulta.AGENDADA, "Revisão do marca-passo", null);
        consulta(camila, rafael, hoje, 14, 0, StatusConsulta.AGENDADA, null, null);

        consulta(pedro, helena, hoje, 9, 0, StatusConsulta.CONFIRMADA, "Retorno - asma", null);
        consulta(davi, helena, hoje, 10, 0, StatusConsulta.AGENDADA, "Puericultura", null);

        consulta(thiago, caio, hoje, 11, 0, StatusConsulta.AGENDADA, "Dor no joelho direito", null);
        consulta(beatriz, caio, hoje, 16, 30, StatusConsulta.AGENDADA, null, null);

        // ---- Amanha ----
        LocalDate amanha = hoje.plusDays(1);
        consulta(larissa, ana, amanha, 9, 0, StatusConsulta.AGENDADA, "Retorno com exames", null);
        consulta(raimundo, rafael, amanha, 10, 0, StatusConsulta.AGENDADA, null, null);
        consulta(davi, helena, amanha, 8, 30, StatusConsulta.AGENDADA, "Vacinas em dia?", null);
    }

    private Paciente paciente(String nome, String cpf, LocalDate nascimento, String telefone, String alertas) {
        return pacientes.save(new Paciente(nome, cpf, nascimento, telefone, null, alertas));
    }

    private Consulta consulta(Paciente paciente, Profissional profissional, LocalDate dia, int hora, int minuto,
            StatusConsulta status, String observacao, String motivoCancelamento) {
        return consultas.salvar(Consulta.reconstituir(null, paciente.getId(), profissional.getId(),
                dia.atTime(LocalTime.of(hora, minuto)), Consulta.DURACAO_MINUTOS, status, observacao,
                motivoCancelamento));
    }

    private void historico(Paciente paciente, Profissional profissional, LocalDate dia, int hora, int minuto,
            String queixa, String evolucao, String conduta) {
        Consulta consulta = consulta(paciente, profissional, dia, hora, minuto, StatusConsulta.REALIZADA, null, null);
        LocalDateTime inicio = dia.atTime(hora, minuto).plusMinutes(3);
        atendimentos.salvar(Atendimento.reconstituir(null, consulta.getId(), paciente.getId(), profissional.getId(),
                inicio, inicio.plusMinutes(22), queixa, evolucao, conduta, StatusAtendimento.FINALIZADO));
    }
}
