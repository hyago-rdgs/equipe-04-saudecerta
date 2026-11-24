package br.edu.ifpa.saudecerta.prontuario.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.edu.ifpa.saudecerta.compartilhado.erro.AcessoNegadoException;
import br.edu.ifpa.saudecerta.compartilhado.erro.RegraDeNegocioException;

class AtendimentoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 16, 9, 0);
    private static final Long PROFISSIONAL = 10L;

    private static Atendimento novo() {
        return Atendimento.iniciar(100L, 1L, PROFISSIONAL, AGORA);
    }

    @Test
    @DisplayName("nao finaliza sem evolucao clinica")
    void finalizarExigeEvolucao() {
        Atendimento atendimento = novo();
        atendimento.registrar("Dor de cabeça", "   ", null);

        assertThatThrownBy(() -> atendimento.finalizar(AGORA.plusMinutes(20)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("evolução");
    }

    @Test
    @DisplayName("finalizado vira documento: nao aceita mais alteracao")
    void finalizadoEhImutavel() {
        Atendimento atendimento = novo();
        atendimento.registrar("Dor de cabeça", "Exame normal.", "Repouso.");
        atendimento.finalizar(AGORA.plusMinutes(20));

        assertThat(atendimento.getStatus()).isEqualTo(StatusAtendimento.FINALIZADO);
        assertThat(atendimento.getFim()).isEqualTo(AGORA.plusMinutes(20));
        assertThatThrownBy(() -> atendimento.registrar("x", "y", "z")).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> atendimento.finalizar(AGORA.plusMinutes(30))).isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    @DisplayName("so o profissional responsavel altera o atendimento")
    void exigeResponsavel() {
        Atendimento atendimento = novo();

        assertThatCode(() -> atendimento.exigirResponsavel(PROFISSIONAL)).doesNotThrowAnyException();
        assertThatThrownBy(() -> atendimento.exigirResponsavel(99L)).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> atendimento.exigirResponsavel(null)).isInstanceOf(AcessoNegadoException.class);
    }
}
