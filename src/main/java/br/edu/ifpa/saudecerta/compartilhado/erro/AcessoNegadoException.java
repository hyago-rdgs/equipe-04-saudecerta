package br.edu.ifpa.saudecerta.compartilhado.erro;

/** O usuario esta autenticado, mas nao pode executar esta operacao (HTTP 403). */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
