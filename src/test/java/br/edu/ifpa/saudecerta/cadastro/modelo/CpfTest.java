package br.edu.ifpa.saudecerta.cadastro.modelo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CpfTest {

    @Test
    void aceitaCpfValidoComOuSemMascara() {
        assertThat(Cpf.valido("15881399803")).isTrue();
        assertThat(Cpf.valido("158.813.998-03")).isTrue();
    }

    @Test
    void recusaDigitoVerificadorErrado() {
        assertThat(Cpf.valido("15881399804")).isFalse();
    }

    @Test
    void recusaSequenciasRepetidasETamanhoErrado() {
        assertThat(Cpf.valido("11111111111")).isFalse();
        assertThat(Cpf.valido("1588139980")).isFalse();
        assertThat(Cpf.valido(null)).isFalse();
    }

    @Test
    void formataComMascara() {
        assertThat(Cpf.formatar("15881399803")).isEqualTo("158.813.998-03");
    }
}
