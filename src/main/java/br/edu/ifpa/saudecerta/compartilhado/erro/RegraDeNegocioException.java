package br.edu.ifpa.saudecerta.compartilhado.erro;

/** Uma regra do dominio foi violada (HTTP 422). */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
