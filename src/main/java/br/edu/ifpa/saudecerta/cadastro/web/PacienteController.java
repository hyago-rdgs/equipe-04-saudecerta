package br.edu.ifpa.saudecerta.cadastro.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifpa.saudecerta.cadastro.aplicacao.PacienteDados;
import br.edu.ifpa.saudecerta.cadastro.aplicacao.PacienteService;
import br.edu.ifpa.saudecerta.cadastro.modelo.Paciente;
import br.edu.ifpa.saudecerta.seguranca.api.UsuarioAtual;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final PacienteService pacientes;

    public PacienteController(PacienteService pacientes) {
        this.pacientes = pacientes;
    }

    @GetMapping
    public List<PacienteResposta> buscar(@RequestParam(required = false) String busca) {
        return pacientes.buscar(busca).stream().map(PacienteResposta::de).toList();
    }

    @GetMapping("/{id}")
    public PacienteResposta obter(@PathVariable Long id) {
        return PacienteResposta.de(pacientes.obter(id));
    }

    @PostMapping
    public ResponseEntity<PacienteResposta> criar(@Valid @RequestBody PacienteDados dados,
            @AuthenticationPrincipal Jwt token) {
        Paciente criado = pacientes.criar(dados, UsuarioAtual.de(token));
        return ResponseEntity.created(URI.create("/api/pacientes/" + criado.getId()))
                .body(PacienteResposta.de(criado));
    }

    @PutMapping("/{id}")
    public PacienteResposta alterar(@PathVariable Long id, @Valid @RequestBody PacienteDados dados,
            @AuthenticationPrincipal Jwt token) {
        return PacienteResposta.de(pacientes.alterar(id, dados, UsuarioAtual.de(token)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativar(@PathVariable Long id, @AuthenticationPrincipal Jwt token) {
        pacientes.inativar(id, UsuarioAtual.de(token));
        return ResponseEntity.noContent().build();
    }
}
