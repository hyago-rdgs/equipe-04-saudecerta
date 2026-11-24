package br.edu.ifpa.saudecerta.agenda.adaptador.web;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifpa.saudecerta.agenda.api.ConsultaInfo;
import br.edu.ifpa.saudecerta.agenda.aplicacao.AgendaService;
import br.edu.ifpa.saudecerta.agenda.aplicacao.NovaConsulta;
import br.edu.ifpa.saudecerta.seguranca.api.UsuarioAtual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** ADAPTADOR DE ENTRADA: traduz HTTP em chamadas aos casos de uso. Nao decide nada. */
@RestController
public class AgendaController {

    public record Cancelamento(@NotBlank(message = "Informe o motivo do cancelamento") String motivo) {
    }

    public record Remarcacao(@NotNull(message = "Escolha o novo horário") LocalDateTime inicio) {
    }

    private final AgendaService agenda;

    public AgendaController(AgendaService agenda) {
        this.agenda = agenda;
    }

    /** Consultas de um dia; opcionalmente de um unico profissional. */
    @GetMapping("/api/agenda")
    public List<ConsultaInfo> doDia(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) Long profissionalId) {
        return agenda.consultasDoDia(data, profissionalId);
    }

    @GetMapping("/api/agenda/horarios-livres")
    public List<LocalTime> horariosLivres(@RequestParam Long profissionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return agenda.horariosLivres(profissionalId, data);
    }

    @PostMapping("/api/consultas")
    public ResponseEntity<ConsultaInfo> agendar(@Valid @RequestBody NovaConsulta pedido,
            @AuthenticationPrincipal Jwt token) {
        ConsultaInfo criada = agenda.agendar(pedido, UsuarioAtual.de(token));
        return ResponseEntity.created(URI.create("/api/consultas/" + criada.id())).body(criada);
    }

    @PostMapping("/api/consultas/{id}/chegada")
    public ConsultaInfo confirmarChegada(@PathVariable Long id, @AuthenticationPrincipal Jwt token) {
        return agenda.confirmarChegada(id, UsuarioAtual.de(token));
    }

    @PostMapping("/api/consultas/{id}/cancelamento")
    public ConsultaInfo cancelar(@PathVariable Long id, @Valid @RequestBody Cancelamento dados,
            @AuthenticationPrincipal Jwt token) {
        return agenda.cancelar(id, dados.motivo(), UsuarioAtual.de(token));
    }

    @PutMapping("/api/consultas/{id}/horario")
    public ConsultaInfo remarcar(@PathVariable Long id, @Valid @RequestBody Remarcacao dados,
            @AuthenticationPrincipal Jwt token) {
        return agenda.remarcar(id, dados.inicio(), UsuarioAtual.de(token));
    }
}
