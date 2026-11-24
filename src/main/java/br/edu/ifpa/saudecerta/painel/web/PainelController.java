package br.edu.ifpa.saudecerta.painel.web;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifpa.saudecerta.compartilhado.erro.MissaoPendenteException;
import br.edu.ifpa.saudecerta.painel.aplicacao.PainelDoDia;
import br.edu.ifpa.saudecerta.painel.aplicacao.PainelService;
import br.edu.ifpa.saudecerta.seguranca.api.UsuarioAtual;

@RestController
public class PainelController {

    private final PainelService painel;

    public PainelController(PainelService painel) {
        this.painel = painel;
    }

    @GetMapping("/api/painel")
    public PainelDoDia hoje(@AuthenticationPrincipal Jwt token) {
        return painel.hoje(UsuarioAtual.de(token));
    }

    /** MISSAO E3-3. */
    @GetMapping("/api/relatorios/consultas")
    public Object relatorio(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return painel.relatorioDeConsultas(de, ate);
    }

    /** MISSAO E3-4: portal do paciente (meus agendamentos). */
    @GetMapping("/api/portal/consultas")
    public Object minhasConsultas(@AuthenticationPrincipal Jwt token) {
        throw new MissaoPendenteException("E3-4", "Portal do paciente ainda não implementado. Veja a missão E3-4 em MISSOES.md.");
    }
}
