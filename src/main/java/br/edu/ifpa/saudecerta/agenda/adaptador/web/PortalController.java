package br.edu.ifpa.saudecerta.agenda.adaptador.web;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifpa.saudecerta.agenda.aplicacao.AgendaService;
import br.edu.ifpa.saudecerta.agenda.aplicacao.ConsultaDoPaciente;
import br.edu.ifpa.saudecerta.seguranca.api.UsuarioAtual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/portal/consultas")
public class PortalController {

    public record Cancelamento(@NotBlank(message = "Informe o motivo do cancelamento.")
            @Size(max = 200, message = "O motivo do cancelamento deve ter até 200 caracteres.") String motivo) {
    }

    private final AgendaService agenda;

    public PortalController(AgendaService agenda) {
        this.agenda = agenda;
    }

    @GetMapping
    public List<ConsultaDoPaciente> minhasConsultas(@AuthenticationPrincipal Jwt token) {
        return agenda.minhasConsultas(UsuarioAtual.de(token));
    }

    @PostMapping("/{id}/cancelamento")
    public ConsultaDoPaciente cancelar(@PathVariable Long id, @Valid @RequestBody Cancelamento dados,
            @AuthenticationPrincipal Jwt token) {
        return agenda.cancelarPeloPortal(id, dados.motivo(), UsuarioAtual.de(token));
    }
}
