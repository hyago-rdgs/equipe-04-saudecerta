package br.edu.ifpa.saudecerta.seguranca.config;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Quem pode o que. As regras ficam em UM lugar, legiveis de cima para baixo
 * (a primeira que casar com a requisicao vence).
 *
 * Perfis: ADMIN, RECEPCAO, PROFISSIONAL, PACIENTE.
 * Repare que o ADMIN NAO le prontuario: administrar o sistema nao da direito
 * de ver dado clinico (LGPD, principio da necessidade).
 */
@Configuration
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String RECEPCAO = "RECEPCAO";
    private static final String PROFISSIONAL = "PROFISSIONAL";
    private static final String PACIENTE = "PACIENTE";

    private final SecretKey chave;

    public SecurityConfig(@Value("${saudecerta.jwt.segredo:}") String segredo) {
        byte[] bytes;
        if (segredo == null || segredo.isBlank()) {
            // Sem segredo configurado: chave aleatoria por execucao.
            // Consequencia: reiniciar o servidor invalida os tokens emitidos.
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
        } else {
            bytes = segredo.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) {
                throw new IllegalStateException("saudecerta.jwt.segredo precisa ter pelo menos 32 caracteres.");
            }
        }
        this.chave = new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public SecurityFilterChain regrasDeAcesso(HttpSecurity http) throws Exception {
        http
                // API sem estado e sem cookies: o token vai no cabecalho Authorization,
                // entao CSRF nao se aplica.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(regras -> regras
                        // Interface web e documentacao
                        .requestMatchers("/", "/index.html", "/favicon.svg", "/css/**", "/js/**", "/error").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()

                        // Prontuario: somente profissionais de saude
                        .requestMatchers("/api/pacientes/*/prontuario").hasRole(PROFISSIONAL)
                        .requestMatchers("/api/atendimentos/**").hasRole(PROFISSIONAL)

                        // Auditoria: somente administracao
                        .requestMatchers("/api/auditoria/**").hasRole(ADMIN)

                        // Cadastros
                        .requestMatchers(HttpMethod.GET, "/api/pacientes/**").hasAnyRole(ADMIN, RECEPCAO, PROFISSIONAL)
                        .requestMatchers("/api/pacientes/**").hasAnyRole(ADMIN, RECEPCAO)
                        .requestMatchers(HttpMethod.GET, "/api/profissionais/**", "/api/especialidades/**")
                        .hasAnyRole(ADMIN, RECEPCAO, PROFISSIONAL)
                        .requestMatchers("/api/profissionais/**", "/api/especialidades/**").hasRole(ADMIN)

                        // Agenda
                        .requestMatchers(HttpMethod.GET, "/api/agenda/**").hasAnyRole(ADMIN, RECEPCAO, PROFISSIONAL)
                        .requestMatchers("/api/consultas/**").hasAnyRole(ADMIN, RECEPCAO)

                        .requestMatchers("/api/painel/**").hasAnyRole(ADMIN, RECEPCAO, PROFISSIONAL)
                        .requestMatchers("/api/relatorios/**").hasRole(ADMIN)
                        .requestMatchers("/api/portal/**").hasRole(PACIENTE)

                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(conversorDePerfis())));
        return http.build();
    }

    /** O token carrega a lista "perfis"; cada item vira uma autoridade ROLE_xxx. */
    private static JwtAuthenticationConverter conversorDePerfis() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("perfis");
        autoridades.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(autoridades);
        return conversor;
    }

    @Bean
    public JwtEncoder emissorDeTokens() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave));
    }

    @Bean
    public JwtDecoder leitorDeTokens() {
        return NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public PasswordEncoder codificadorDeSenhas() {
        return new BCryptPasswordEncoder();
    }
}
