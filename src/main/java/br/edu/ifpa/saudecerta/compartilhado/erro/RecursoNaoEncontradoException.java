package br.edu.ifpa.saudecerta.compartilhado.erro;

/** O recurso pedido nao existe (HTTP 404). */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
