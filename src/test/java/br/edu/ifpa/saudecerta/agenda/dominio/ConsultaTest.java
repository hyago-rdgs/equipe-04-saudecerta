package br.edu.ifpa.saudecerta.agenda.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.edu.ifpa.saudecerta.compartilhado.erro.RegraDeNegocioException;

/**
 * Testes de DOMINIO: rodam em milissegundos, sem Spring e sem banco.
 * Isso so e possivel porque Consulta nao depende de framework nenhum.
 */
class ConsultaTest {

    /** Segunda-feira, 16/11/2026, 08:00. */
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 16, 8, 0);

    private static Consulta consultaAs(int hora, int minuto) {
        return Consulta.agendar(1L, 10L, AGORA.withHour(hora).withMinute(minuto), null, AGORA);
    }

    @Test
    @DisplayName("consulta nova nasce AGENDADA e dura 30 minutos")
    void nasceAgendada() {
        Consulta consulta = consultaAs(10, 0);

        assertThat(consulta.getStatus()).isEqualTo(StatusConsulta.AGENDADA);
        assertThat(consulta.fim()).isEqualTo(AGORA.withHour(10).withMinute(30));
    }

    @Test
    @DisplayName("nao agenda em horario que ja passou")
    void recusaPassado() {
        assertThatThrownBy(() -> consultaAs(7, 30))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("já passou");
    }

    @Test
    @DisplayName("nao agenda fora da grade de 30 minutos")
    void recusaHorarioQuebrado() {
        assertThatThrownBy(() -> consultaAs(10, 15)).isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    @DisplayName("nao agenda fora do horario de funcionamento")
    void recusaForaDoExpediente() {
        assertThatThrownBy(() -> consultaAs(19, 0)).isInstanceOf(RegraDeNegocioException.class);
        assertThat(consultaAs(18, 30).getStatus()).isEqualTo(StatusConsulta.AGENDADA); // ultimo horario do dia
    }

    @Test
    @DisplayName("nao agenda aos domingos")
    void recusaDomingo() {
        LocalDateTime domingo = LocalDateTime.of(2026, 11, 22, 10, 0);

        assertThatThrownBy(() -> Consulta.agendar(1L, 10L, domingo, null, AGORA))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("domingos");
    }

    @Test
    @DisplayName("consultas no mesmo horario conflitam; em horarios vizinhos, nao")
    void detectaConflito() {
        Consulta dez = consultaAs(10, 0);

        assertThat(dez.conflitaCom(consultaAs(10, 0))).isTrue();
        assertThat(dez.conflitaCom(consultaAs(10, 30))).isFalse();
        assertThat(dez.conflitaCom(consultaAs(9, 30))).isFalse();
    }

    @Test
    @DisplayName("consulta cancelada libera o horario")
    void canceladaNaoConflita() {
        Consulta cancelada = consultaAs(10, 0);
        cancelada.cancelar("Paciente desistiu");

        assertThat(cancelada.conflitaCom(consultaAs(10, 0))).isFalse();
    }

    @Test
    @DisplayName("ciclo de vida: agendada -> confirmada -> em atendimento -> realizada")
    void cicloDeVida() {
        Consulta consulta = consultaAs(10, 0);

        consulta.confirmarChegada();
        assertThat(consulta.getStatus()).isEqualTo(StatusConsulta.CONFIRMADA);

        consulta.iniciarAtendimento();
        assertThat(consulta.getStatus()).isEqualTo(StatusConsulta.EM_ATENDIMENTO);

        consulta.concluir();
        assertThat(consulta.getStatus()).isEqualTo(StatusConsulta.REALIZADA);
    }

    @Test
    @DisplayName("atendimento nao comeca antes de a recepcao confirmar a chegada")
    void atendimentoExigeChegada() {
        Consulta consulta = consultaAs(10, 0);

        assertThatThrownBy(consulta::iniciarAtendimento).isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    @DisplayName("cancelamento exige motivo e nao vale para consulta em atendimento")
    void regrasDeCancelamento() {
        Consulta semMotivo = consultaAs(10, 0);
        assertThatThrownBy(() -> semMotivo.cancelar("  ")).isInstanceOf(RegraDeNegocioException.class);

        Consulta emAtendimento = consultaAs(11, 0);
        emAtendimento.confirmarChegada();
        emAtendimento.iniciarAtendimento();
        assertThatThrownBy(() -> emAtendimento.cancelar("Tarde demais")).isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void remarcaConsultaAgendadaOuConfirmadaComMesmoIdentificador() {
        Consulta consulta = Consulta.reconstituir(42L, 1L, 10L, AGORA.plusHours(2), 30,
                StatusConsulta.AGENDADA, "Observação", null);
        consulta.remarcar(AGORA.plusHours(3), AGORA);
        consulta.confirmarChegada();
        consulta.remarcar(AGORA.plusDays(1), AGORA);

        assertThat(consulta.getId()).isEqualTo(42L);
        assertThat(consulta.getInicio()).isEqualTo(AGORA.plusDays(1));
        assertThat(consulta.getStatus()).isEqualTo(StatusConsulta.AGENDADA);
        assertThat(consulta.getObservacao()).isEqualTo("Observação");
    }

    @Test
    void recusaRemarcacaoNosDemaisEstados() {
        for (StatusConsulta status : new StatusConsulta[] {
                StatusConsulta.CANCELADA, StatusConsulta.EM_ATENDIMENTO, StatusConsulta.REALIZADA }) {
            Consulta consulta = Consulta.reconstituir(1L, 1L, 10L, AGORA.plusHours(2), 30, status, null, null);
            assertThatThrownBy(() -> consulta.remarcar(AGORA.plusDays(1), AGORA))
                    .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("agendada ou confirmada");
        }
    }

    @Test
    void remarcacaoSegueAsRegrasDaGradeEExigeOutroHorario() {
        Consulta consulta = consultaAs(10, 0);
        assertThatThrownBy(() -> consulta.remarcar(AGORA, AGORA)).hasMessageContaining("já passou");
        assertThatThrownBy(() -> consulta.remarcar(AGORA.plusDays(6), AGORA)).hasMessageContaining("domingos");
        assertThatThrownBy(() -> consulta.remarcar(AGORA.plusMinutes(15), AGORA)).hasMessageContaining("meia hora");
        assertThatThrownBy(() -> consulta.remarcar(AGORA.plusHours(11), AGORA)).hasMessageContaining("funcionamento");
        assertThatThrownBy(() -> consulta.remarcar(AGORA.plusHours(2), AGORA)).hasMessageContaining("diferente");
        assertThat(consulta.getInicio()).isEqualTo(AGORA.plusHours(2));
    }

    @Test
    void pacienteSoCancelaConsultaFuturaComMotivo() {
        Consulta consulta = consultaAs(10, 0);
        assertThatThrownBy(() -> consulta.cancelarPeloPaciente("Imprevisto", AGORA.plusHours(2)))
                .hasMessageContaining("futuras");
        assertThatThrownBy(() -> consulta.cancelarPeloPaciente("   ", AGORA)).hasMessageContaining("motivo");

        consulta.cancelarPeloPaciente("  Imprevisto  ", AGORA);

        assertThat(consulta.getStatus()).isEqualTo(StatusConsulta.CANCELADA);
        assertThat(consulta.getMotivoCancelamento()).isEqualTo("Imprevisto");
    }
}
