package br.edu.ifpa.saudecerta.compartilhado.erro;

/** O pedido conflita com o estado atual (HTTP 409). Ex.: horario ja ocupado, CPF duplicado. */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
