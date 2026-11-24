package br.edu.ifpa.saudecerta.compartilhado.erro;

/**
 * Funcionalidade deixada para a equipe implementar (HTTP 501).
 * O codigo aponta para a missao correspondente em MISSOES.md.
 */
public class MissaoPendenteException extends RuntimeException {

    private final String missao;

    public MissaoPendenteException(String missao, String mensagem) {
        super(mensagem);
        this.missao = missao;
    }

    public String getMissao() {
        return missao;
    }
}
