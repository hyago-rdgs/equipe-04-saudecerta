package br.edu.ifpa.saudecerta.compartilhado.raiox;

import java.io.IOException;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Abre um rastro para cada chamada a /api e devolve o identificador no
 * cabecalho X-Raio-X. A tela usa esse identificador para buscar o rastro.
 */
@Component
@ConditionalOnProperty(name = "saudecerta.raio-x.ativo", havingValue = "true")
public class RaioXFilter extends OncePerRequestFilter {

    public static final String CABECALHO = "X-Raio-X";

    private final RaioXArmazem armazem;

    public RaioXFilter(RaioXArmazem armazem) {
        this.armazem = armazem;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String caminho = request.getRequestURI();
        return !caminho.startsWith("/api/") || caminho.startsWith("/api/raio-x");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = UUID.randomUUID().toString().substring(0, 8);
        Rastro rastro = new Rastro(id, request.getMethod(), request.getRequestURI());
        response.setHeader(CABECALHO, id);
        RaioXContexto.iniciar(rastro);
        long inicio = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            rastro.concluir(response.getStatus(), System.nanoTime() - inicio);
            armazem.guardar(rastro);
            RaioXContexto.limpar();
        }
    }
}
