package br.edu.ifpa.saudecerta.seguranca.aplicacao;

public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("Usuário ou senha incorretos.");
    }
}
