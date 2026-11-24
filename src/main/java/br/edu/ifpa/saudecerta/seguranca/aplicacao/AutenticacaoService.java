package br.edu.ifpa.saudecerta.seguranca.aplicacao;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifpa.saudecerta.auditoria.api.Auditoria;
import br.edu.ifpa.saudecerta.auditoria.api.EventoDeAuditoria;
import br.edu.ifpa.saudecerta.seguranca.api.UsuarioLogado;
import br.edu.ifpa.saudecerta.seguranca.modelo.Usuario;
import br.edu.ifpa.saudecerta.seguranca.persistencia.UsuarioRepository;

@Service
public class AutenticacaoService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;
    private final JwtEncoder emissor;
    private final Auditoria auditoria;
    private final Clock relogio;
    private final Duration validade;

    public AutenticacaoService(UsuarioRepository usuarios, PasswordEncoder codificador, JwtEncoder emissor,
            Auditoria auditoria, Clock relogio,
            @Value("${saudecerta.jwt.expiracao-minutos:480}") long expiracaoMinutos) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.emissor = emissor;
        this.auditoria = auditoria;
        this.relogio = relogio;
        this.validade = Duration.ofMinutes(expiracaoMinutos);
    }

    @Transactional
    public Sessao entrar(String login, String senha) {
        // A mesma mensagem para "usuario nao existe" e "senha errada":
        // nao revelamos quais logins sao validos.
        Usuario usuario = usuarios.findByLogin(login == null ? "" : login.trim().toLowerCase())
                .filter(u -> codificador.matches(senha == null ? "" : senha, u.getSenhaHash()))
                .orElseThrow(() -> {
                    auditoria.registrar(new EventoDeAuditoria(login, null, "LOGIN_RECUSADO", "Sessão", null, null,
                            "Tentativa de acesso com credenciais inválidas"));
                    return new CredenciaisInvalidasException();
                });

        Instant agora = relogio.instant();
        Instant expiraEm = agora.plus(validade);
        JwtClaimsSet.Builder dados = JwtClaimsSet.builder()
                .subject(usuario.getLogin())
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .claim("nome", usuario.getNome())
                .claim("perfis", List.of(usuario.getPerfil().name()));
        if (usuario.getProfissionalId() != null) {
            dados.claim("profissionalId", usuario.getProfissionalId());
        }
        if (usuario.getPacienteId() != null) {
            dados.claim("pacienteId", usuario.getPacienteId());
        }
        String token = emissor
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), dados.build()))
                .getTokenValue();

        UsuarioLogado logado = new UsuarioLogado(usuario.getLogin(), usuario.getNome(), usuario.getPerfil(),
                usuario.getProfissionalId(), usuario.getPacienteId());
        auditoria.registrar(new EventoDeAuditoria(logado.login(), logado.perfil().name(), "LOGIN", "Sessão", null,
                null, "Acesso ao sistema"));
        return new Sessao(token, expiraEm, logado);
    }
}
