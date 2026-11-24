package br.edu.ifpa.saudecerta.agenda.aplicacao;

import static br.edu.ifpa.saudecerta.apoio.CenarioEmMemoria.AGORA;
import static br.edu.ifpa.saudecerta.apoio.CenarioEmMemoria.JOANA;
import static br.edu.ifpa.saudecerta.apoio.CenarioEmMemoria.RECEPCAO;
import static br.edu.ifpa.saudecerta.apoio.CenarioEmMemoria.RELOGIO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import br.edu.ifpa.saudecerta.agenda.dominio.Consulta;
import br.edu.ifpa.saudecerta.apoio.CenarioEmMemoria.CadastroEmMemoria;
import br.edu.ifpa.saudecerta.apoio.ConsultasEmMemoria;
import br.edu.ifpa.saudecerta.auditoria.api.EventoDeAuditoria;
import br.edu.ifpa.saudecerta.compartilhado.erro.ConflitoException;
import br.edu.ifpa.saudecerta.compartilhado.erro.RecursoNaoEncontradoException;
import br.edu.ifpa.saudecerta.compartilhado.erro.RegraDeNegocioException;

class AgendaServiceTest {

    private static final LocalDateTime HORARIO = AGORA.plusHours(3);

    private final ConsultasEmMemoria consultas = new ConsultasEmMemoria();
    private final List<EventoDeAuditoria> eventos = new ArrayList<>();
    private final AgendaService agenda = new AgendaService(consultas, new CadastroEmMemoria(), eventos::add, RELOGIO);

    @Test
    void agendaConsultaComNomesERegistraAuditoria() {
        var criada = agenda.agendar(pedido(1L, 1L), RECEPCAO);

        assertThat(criada.id()).isNotNull();
        assertThat(criada.inicio()).isEqualTo(HORARIO);
        assertThat(criada.status()).isEqualTo("AGENDADA");
        assertThat(criada.paciente()).isEqualTo("Joana");
        assertThat(criada.profissional()).isEqualTo("Dra. Ana");
        assertThat(consultas.buscarPorId(criada.id())).isPresent();
        assertThat(eventos).extracting(EventoDeAuditoria::acao).containsExactly("CONSULTA_AGENDADA");
    }

    @Test
    void recusaConflitosDeProfissionalEPacienteEPacienteInativo() {
        agenda.agendar(pedido(1L, 1L), RECEPCAO);

        assertThatThrownBy(() -> agenda.agendar(pedido(2L, 1L), RECEPCAO))
                .isInstanceOf(ConflitoException.class).hasMessageContaining("Dra. Ana");
        assertThatThrownBy(() -> agenda.agendar(pedido(1L, 2L), RECEPCAO))
                .isInstanceOf(ConflitoException.class).hasMessageContaining("Joana");
        assertThatThrownBy(() -> agenda.agendar(pedido(3L, 2L), RECEPCAO))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("inativo");

        assertThat(consultas.doDia(HORARIO.toLocalDate())).hasSize(1);
        assertThat(eventos).hasSize(1);
    }

    @Test
    void remarcaConsultaConfirmadaEVoltaParaAgendada() {
        var criada = agenda.agendar(pedido(1L, 1L), RECEPCAO);
        agenda.confirmarChegada(criada.id(), RECEPCAO);
        var novoHorario = HORARIO.plusMinutes(30);

        var remarcada = agenda.remarcar(criada.id(), novoHorario, RECEPCAO);

        assertThat(remarcada.id()).isEqualTo(criada.id());
        assertThat(remarcada.inicio()).isEqualTo(novoHorario);
        assertThat(remarcada.status()).isEqualTo("AGENDADA");
        assertThat(eventos.get(2).detalhe()).contains(HORARIO.toString(), novoHorario.toString());
    }

    @Test
    void portalCancelaSoConsultaPropriaELiberaHorario() {
        var propria = agenda.agendar(pedido(1L, 1L), RECEPCAO);
        var alheia = agenda.agendar(pedido(2L, 2L), RECEPCAO);

        assertThat(agenda.minhasConsultas(JOANA)).extracting(ConsultaDoPaciente::id).containsExactly(propria.id());
        assertThatThrownBy(() -> agenda.cancelarPeloPortal(alheia.id(), "Imprevisto", JOANA))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThat(agenda.cancelarPeloPortal(propria.id(), "Imprevisto", JOANA).status()).isEqualTo("CANCELADA");
        assertThat(agenda.agendar(pedido(1L, 1L), RECEPCAO).id()).isNotEqualTo(propria.id());
    }

    @Test
    void reproduzReservaDuplicadaQuandoAsDuasThreadsValidamSemBloqueio() throws Exception {
        var ambasJaLeram = new CyclicBarrier(2);
        var semBloqueio = new ConsultasEmMemoria() {
            @Override
            public List<Consulta> doPacienteNoDia(Long pacienteId, LocalDate data) {
                var lidas = super.doPacienteNoDia(pacienteId, data);
                // Ambas capturam o resultado da última leitura antes de qualquer gravação.
                try {
                    ambasJaLeram.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
                return lidas;
            }
        };
        var agendaSemBloqueio = new AgendaService(semBloqueio, new CadastroEmMemoria(), evento -> {}, RELOGIO);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var uma = executor.submit(() -> agendaSemBloqueio.agendar(pedido(1L, 1L), RECEPCAO));
            var outra = executor.submit(() -> agendaSemBloqueio.agendar(pedido(2L, 1L), RECEPCAO));
            assertThat(uma.get(10, TimeUnit.SECONDS).id()).isNotEqualTo(outra.get(10, TimeUnit.SECONDS).id());
            assertThat(semBloqueio.doProfissionalNoDia(1L, HORARIO.toLocalDate())).hasSize(2);
        } finally {
            executor.shutdownNow();
        }
    }

    private static NovaConsulta pedido(Long pacienteId, Long profissionalId) {
        return new NovaConsulta(pacienteId, profissionalId, HORARIO, null);
    }
}
