package br.edu.ifpa.saudecerta.seguranca.aplicacao;

import java.time.Instant;

import br.edu.ifpa.saudecerta.seguranca.api.UsuarioLogado;

/** Resultado de um login bem-sucedido. */
public record Sessao(String token, Instant expiraEm, UsuarioLogado usuario) {
}
