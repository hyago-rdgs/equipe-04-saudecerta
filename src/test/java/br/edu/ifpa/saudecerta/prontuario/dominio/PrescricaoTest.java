package br.edu.ifpa.saudecerta.prontuario.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.edu.ifpa.saudecerta.compartilhado.erro.RegraDeNegocioException;
import br.edu.ifpa.saudecerta.prontuario.dominio.Prescricao.Item;

/**
 * MISSAO E3-2 - a especificacao da prescricao, escrita como teste.
 *
 * Passo a passo: apague a linha @Disabled, rode "mvn test", veja os testes
 * falharem e implemente Prescricao ate todos passarem. So depois va para o
 * caso de uso, a persistencia e a API.
 */
@Disabled("Missão E3-2: remova esta linha e implemente Prescricao")
class PrescricaoTest {

    private static final Item AMOXICILINA = new Item("Amoxicilina 500 mg", "1 cápsula", "de 8 em 8 horas por 7 dias");

    @Test
    @DisplayName("aceita item completo")
    void aceitaItemCompleto() {
        Prescricao prescricao = new Prescricao(1L);

        prescricao.adicionar(AMOXICILINA, null);

        assertThat(prescricao.getItens()).containsExactly(AMOXICILINA);
    }

    @Test
    @DisplayName("recusa item sem medicamento, dose ou posologia")
    void recusaItemIncompleto() {
        Prescricao prescricao = new Prescricao(1L);

        assertThatThrownBy(() -> prescricao.adicionar(new Item(" ", "1 cápsula", "de 8 em 8 horas"), null))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> prescricao.adicionar(new Item("Amoxicilina 500 mg", null, "de 8 em 8 horas"), null))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> prescricao.adicionar(new Item("Amoxicilina 500 mg", "1 cápsula", ""), null))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    @DisplayName("recusa o mesmo medicamento duas vezes")
    void recusaMedicamentoRepetido() {
        Prescricao prescricao = new Prescricao(1L);
        prescricao.adicionar(AMOXICILINA, null);

        assertThatThrownBy(() -> prescricao.adicionar(new Item("amoxicilina 500 MG", "2 cápsulas", "1 vez ao dia"), null))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    @DisplayName("recusa medicamento citado nos alertas de alergia do paciente")
    void recusaMedicamentoComAlergia() {
        Prescricao prescricao = new Prescricao(1L);

        assertThatThrownBy(() -> prescricao.adicionar(new Item("Dipirona 1 g", "1 comprimido", "se dor"),
                "Alergia a dipirona"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("alergia");
    }

    @Test
    @DisplayName("prescricao vazia nao pode ser emitida")
    void recusaPrescricaoVazia() {
        assertThatThrownBy(() -> new Prescricao(1L).validarParaEmissao())
                .isInstanceOf(RegraDeNegocioException.class);
    }
}
