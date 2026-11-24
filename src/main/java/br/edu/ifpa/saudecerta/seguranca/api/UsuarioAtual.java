package br.edu.ifpa.saudecerta.seguranca.api;

import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;


/**
 * Converte o token JWT (detalhe de infraestrutura) em UsuarioLogado
 * (conceito da aplicacao). Os controllers chamam isto e repassam o resultado.
 */
public final class UsuarioAtual {

    private UsuarioAtual() {
    }

    public static UsuarioLogado de(Jwt token) {
        List<String> perfis = token.getClaimAsStringList("perfis");
        Perfil perfil = Perfil.valueOf(perfis.get(0));
        return new UsuarioLogado(token.getSubject(), token.getClaimAsString("nome"), perfil,
                numero(token, "profissionalId"), numero(token, "pacienteId"));
    }

    private static Long numero(Jwt token, String nome) {
        Object valor = token.getClaims().get(nome);
        return valor instanceof Number n ? n.longValue() : null;
    }
}
