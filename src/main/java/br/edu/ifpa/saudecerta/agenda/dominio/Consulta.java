package br.edu.ifpa.saudecerta.agenda.dominio;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

import br.edu.ifpa.saudecerta.compartilhado.erro.RegraDeNegocioException;

/**
 * Consulta agendada. Classe de DOMINIO: Java puro, sem anotacoes de framework.
 * Todas as regras da agenda moram aqui e podem ser testadas sem subir o Spring
 * nem o banco (veja ConsultaTest).
 */
public class Consulta {

    public static final int DURACAO_MINUTOS = 30;
    public static final LocalTime ABERTURA = LocalTime.of(7, 0);
    public static final LocalTime FECHAMENTO = LocalTime.of(19, 0);

    private final Long id;
    private final Long pacienteId;
    private final Long profissionalId;
    private final LocalDateTime inicio;
    private final int duracaoMinutos;
    private StatusConsulta status;
    private final String observacao;
    private String motivoCancelamento;

    private Consulta(Long id, Long pacienteId, Long profissionalId, LocalDateTime inicio, int duracaoMinutos,
            StatusConsulta status, String observacao, String motivoCancelamento) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.profissionalId = profissionalId;
        this.inicio = inicio;
        this.duracaoMinutos = duracaoMinutos;
        this.status = status;
        this.observacao = observacao;
        this.motivoCancelamento = motivoCancelamento;
    }

    /** Cria uma nova consulta, validando as regras de horario da clinica. */
    public static Consulta agendar(Long pacienteId, Long profissionalId, LocalDateTime inicio, String observacao,
            LocalDateTime agora) {
        if (pacienteId == null || profissionalId == null || inicio == null) {
            throw new RegraDeNegocioException("Paciente, profissional e horário são obrigatórios.");
        }
        if (!inicio.isAfter(agora)) {
            throw new RegraDeNegocioException("Não é possível agendar em um horário que já passou.");
        }
        if (inicio.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new RegraDeNegocioException("A clínica não atende aos domingos.");
        }
        if (inicio.getMinute() % DURACAO_MINUTOS != 0 || inicio.getSecond() != 0 || inicio.getNano() != 0) {
            throw new RegraDeNegocioException("As consultas começam em horários cheios ou meia hora (ex.: 08:00, 08:30).");
        }
        LocalTime hora = inicio.toLocalTime();
        if (hora.isBefore(ABERTURA) || hora.plusMinutes(DURACAO_MINUTOS).isAfter(FECHAMENTO)) {
            throw new RegraDeNegocioException("Horário fora do funcionamento da clínica (07:00 às 19:00).");
        }
        return new Consulta(null, pacienteId, profissionalId, inicio, DURACAO_MINUTOS, StatusConsulta.AGENDADA,
                observacao, null);
    }

    /** Recria uma consulta ja existente (uso exclusivo da persistencia). Nao valida regras de criacao. */
    public static Consulta reconstituir(Long id, Long pacienteId, Long profissionalId, LocalDateTime inicio,
            int duracaoMinutos, StatusConsulta status, String observacao, String motivoCancelamento) {
        return new Consulta(id, pacienteId, profissionalId, inicio, duracaoMinutos, status, observacao,
                motivoCancelamento);
    }

    public LocalDateTime fim() {
        return inicio.plusMinutes(duracaoMinutos);
    }

    /** Consultas canceladas liberam o horario. */
    public boolean ocupaHorario() {
        return status != StatusConsulta.CANCELADA;
    }

    /** Dois intervalos conflitam quando um comeca antes de o outro terminar, e vice-versa. */
    public boolean conflitaCom(Consulta outra) {
        return ocupaHorario() && outra.ocupaHorario()
                && inicio.isBefore(outra.fim()) && outra.inicio.isBefore(fim());
    }

    /** O paciente chegou a recepcao. */
    public void confirmarChegada() {
        exigir(StatusConsulta.AGENDADA, "Só é possível confirmar a chegada de uma consulta agendada.");
        status = StatusConsulta.CONFIRMADA;
    }

    public void iniciarAtendimento() {
        exigir(StatusConsulta.CONFIRMADA,
                "O atendimento só começa depois que a recepção confirma a chegada do paciente.");
        status = StatusConsulta.EM_ATENDIMENTO;
    }

    public void concluir() {
        exigir(StatusConsulta.EM_ATENDIMENTO, "Só é possível concluir uma consulta em atendimento.");
        status = StatusConsulta.REALIZADA;
    }

    public void cancelar(String motivo) {
        if (status != StatusConsulta.AGENDADA && status != StatusConsulta.CONFIRMADA) {
            throw new RegraDeNegocioException("Esta consulta não pode mais ser cancelada (situação: " + status + ").");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new RegraDeNegocioException("Informe o motivo do cancelamento.");
        }
        status = StatusConsulta.CANCELADA;
        motivoCancelamento = motivo.trim();
    }

    private void exigir(StatusConsulta esperado, String mensagem) {
        if (status != esperado) {
            throw new RegraDeNegocioException(mensagem);
        }
    }

    public Long getId() { return id; }
    public Long getPacienteId() { return pacienteId; }
    public Long getProfissionalId() { return profissionalId; }
    public LocalDateTime getInicio() { return inicio; }
    public int getDuracaoMinutos() { return duracaoMinutos; }
    public StatusConsulta getStatus() { return status; }
    public String getObservacao() { return observacao; }
    public String getMotivoCancelamento() { return motivoCancelamento; }
}
