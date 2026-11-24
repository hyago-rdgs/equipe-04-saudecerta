package br.edu.ifpa.saudecerta.seguranca.api;

/**
 * Quem esta fazendo a requisicao. E o unico dado de seguranca que os
 * servicos de aplicacao conhecem: nenhum deles importa Spring Security.
 */
public record UsuarioLogado(String login, String nome, Perfil perfil, Long profissionalId, Long pacienteId) {

    public boolean tem(Perfil esperado) {
        return perfil == esperado;
    }
}
