package br.edu.ifpa.saudecerta.painel.dominio;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import br.edu.ifpa.saudecerta.compartilhado.erro.RegraDeNegocioException;

class PeriodoDoRelatorioTest {

    private static final LocalDate INICIO = LocalDate.of(2024, 2, 29);

    @Test
    void aceitaUmDiaOuAteUmAnoCivil() {
        assertThatCode(() -> new PeriodoDoRelatorio(INICIO, INICIO)).doesNotThrowAnyException();
        assertThatCode(() -> new PeriodoDoRelatorio(INICIO, LocalDate.of(2025, 2, 28)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejeitaFimAnteriorAoInicio() {
        assertThatThrownBy(() -> new PeriodoDoRelatorio(INICIO, INICIO.minusDays(1)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("anterior");
    }

    @Test
    void rejeitaMaisDeUmAnoCivil() {
        assertThatThrownBy(() -> new PeriodoDoRelatorio(INICIO, LocalDate.of(2025, 3, 1)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("um ano");
    }
}
