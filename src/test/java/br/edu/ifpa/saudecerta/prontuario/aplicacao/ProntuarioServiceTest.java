package br.edu.ifpa.saudecerta.prontuario.aplicacao;

import static br.edu.ifpa.saudecerta.apoio.CenarioEmMemoria.AGORA;
import static br.edu.ifpa.saudecerta.apoio.CenarioEmMemoria.ANA;
import static br.edu.ifpa.saudecerta.apoio.CenarioEmMemoria.RAFAEL;
import static br.edu.ifpa.saudecerta.apoio.CenarioEmMemoria.RECEPCAO;
import static br.edu.ifpa.saudecerta.apoio.CenarioEmMemoria.RELOGIO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.edu.ifpa.saudecerta.agenda.aplicacao.AgendaService;
import br.edu.ifpa.saudecerta.agenda.aplicacao.NovaConsulta;
import br.edu.ifpa.saudecerta.apoio.AtendimentosEmMemoria;
import br.edu.ifpa.saudecerta.apoio.CenarioEmMemoria.CadastroEmMemoria;
import br.edu.ifpa.saudecerta.apoio.ConsultasEmMemoria;
import br.edu.ifpa.saudecerta.auditoria.api.EventoDeAuditoria;
import br.edu.ifpa.saudecerta.compartilhado.erro.AcessoNegadoException;
import br.edu.ifpa.saudecerta.compartilhado.erro.RegraDeNegocioException;
import br.edu.ifpa.saudecerta.prontuario.dominio.Prescricao;
import br.edu.ifpa.saudecerta.prontuario.dominio.Prescricao.Item;

class ProntuarioServiceTest {

    private final AtendimentosEmMemoria atendimentos = new AtendimentosEmMemoria();
    private final List<EventoDeAuditoria> eventos = new ArrayList<>();
    private final CadastroEmMemoria cadastro = new CadastroEmMemoria();
    private final AgendaService agenda = new AgendaService(new ConsultasEmMemoria(), cadastro, eventos::add, RELOGIO);
    private final ProntuarioService prontuario = new ProntuarioService(atendimentos, new PrescricoesEmMemoria(), agenda,
            cadastro, eventos::add, RELOGIO);
    private Long consultaId;

    @BeforeEach
    void agendar() {
        consultaId = agenda.agendar(new NovaConsulta(1L, 1L, AGORA.plusHours(1), null), RECEPCAO).id();
        eventos.clear();
    }

    @Test
    void exigeChegadaEProfissionalResponsavelParaIniciar() {
        assertThatThrownBy(() -> prontuario.iniciar(consultaId, RAFAEL)).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> prontuario.iniciar(consultaId, ANA)).isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("recepção confirma");
        assertThat(atendimentos.buscarPorConsulta(consultaId)).isEmpty();
        assertThat(eventos).isEmpty();
    }

    @Test
    void iniciaUmaVezEReabreMesmoAtendimentoComAuditoria() {
        agenda.confirmarChegada(consultaId, RECEPCAO);
        var iniciado = prontuario.iniciar(consultaId, ANA);
        var reaberto = prontuario.iniciar(consultaId, ANA);

        assertThat(iniciado.inicio()).isEqualTo(AGORA);
        assertThat(reaberto.id()).isEqualTo(iniciado.id());
        assertThat(atendimentos.doPaciente(1L)).hasSize(1);
        assertThat(agenda.consulta(consultaId).status()).isEqualTo("EM_ATENDIMENTO");
        assertThat(eventos).extracting(EventoDeAuditoria::acao)
                .containsExactly("CHEGADA_CONFIRMADA", "ATENDIMENTO_INICIADO", "PRONTUARIO_LIDO");
    }

    @Test
    void salvaEFinalizaSomenteComEvolucao() {
        agenda.confirmarChegada(consultaId, RECEPCAO);
        Long id = prontuario.iniciar(consultaId, ANA).id();
        var registro = new RegistroClinico("Queixa fictícia", "Evolução fictícia", null);

        assertThatThrownBy(() -> prontuario.finalizar(id, ANA)).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> prontuario.registrar(id, registro, RAFAEL)).isInstanceOf(AcessoNegadoException.class);

        prontuario.registrar(id, registro, ANA);
        var finalizado = prontuario.finalizar(id, ANA);

        assertThat(finalizado.status()).isEqualTo("FINALIZADO");
        assertThat(finalizado.fim()).isEqualTo(AGORA);
        assertThat(finalizado.evolucao()).isEqualTo("Evolução fictícia");
        assertThat(agenda.consulta(consultaId).status()).isEqualTo("REALIZADA");
        assertThatThrownBy(() -> prontuario.registrar(id, registro, ANA)).isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void validaPrescricaoEExibeNoHistorico() {
        agenda.confirmarChegada(consultaId, RECEPCAO);
        Long id = prontuario.iniciar(consultaId, ANA).id();
        var itemComAlerta = new Item("Dipirona 1 g", "Dose fictícia", "Posologia fictícia");
        var item = new Item("Medicamento fictício", "Dose fictícia", "Posologia fictícia");

        assertThatThrownBy(() -> prontuario.prescrever(id, List.of(itemComAlerta), ANA))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("alergia");
        var emitida = prontuario.prescrever(id, List.of(item), ANA);

        assertThat(emitida.emitidaEm()).isEqualTo(AGORA);
        assertThat(prontuario.lerPrescricoes(id, ANA)).containsExactly(emitida);
        assertThat(prontuario.doPaciente(1L, ANA).atendimentos()).singleElement()
                .satisfies(a -> assertThat(a.prescricoes()).containsExactly(emitida));
        assertThat(eventos).extracting(EventoDeAuditoria::acao).contains(
                "PRESCRICAO_EMITIDA", "PRESCRICOES_LIDAS", "PRONTUARIO_LIDO");
    }

    private static class PrescricoesEmMemoria implements PrescricaoRepositorio {
        private final Map<Long, List<PrescricaoInfo>> porAtendimento = new HashMap<>();
        private long ultimoId;

        @Override
        public PrescricaoInfo salvar(Prescricao prescricao, LocalDateTime emitidaEm) {
            var info = new PrescricaoInfo(++ultimoId, emitidaEm, prescricao.getItens());
            porAtendimento.computeIfAbsent(prescricao.getAtendimentoId(), id -> new ArrayList<>()).add(info);
            return info;
        }

        @Override
        public Map<Long, List<PrescricaoInfo>> dosAtendimentos(Collection<Long> atendimentoIds) {
            var resultado = new HashMap<Long, List<PrescricaoInfo>>();
            atendimentoIds.forEach(id -> resultado.put(id, List.copyOf(porAtendimento.getOrDefault(id, List.of()))));
            return resultado;
        }
    }
}
