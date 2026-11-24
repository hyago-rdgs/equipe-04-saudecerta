package br.edu.ifpa.saudecerta.seguranca.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifpa.saudecerta.seguranca.aplicacao.AutenticacaoService;
import br.edu.ifpa.saudecerta.seguranca.aplicacao.CredenciaisInvalidasException;
import br.edu.ifpa.saudecerta.seguranca.aplicacao.Sessao;
import br.edu.ifpa.saudecerta.seguranca.api.UsuarioAtual;
import br.edu.ifpa.saudecerta.seguranca.api.UsuarioLogado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record Credenciais(@NotBlank(message = "Informe o usuário") String login,
            @NotBlank(message = "Informe a senha") String senha) {
    }

    private final AutenticacaoService autenticacao;

    public AuthController(AutenticacaoService autenticacao) {
        this.autenticacao = autenticacao;
    }

    @PostMapping("/login")
    public Sessao entrar(@Valid @RequestBody Credenciais credenciais) {
        return autenticacao.entrar(credenciais.login(), credenciais.senha());
    }

    @GetMapping("/eu")
    public UsuarioLogado eu(@AuthenticationPrincipal Jwt token) {
        return UsuarioAtual.de(token);
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ProblemDetail credenciaisInvalidas(CredenciaisInvalidasException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
        p.setTitle("Não autenticado");
        return p;
    }
}
