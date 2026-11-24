package br.edu.ifpa.saudecerta.prontuario.adaptador.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifpa.saudecerta.prontuario.aplicacao.AtendimentoInfo;
import br.edu.ifpa.saudecerta.prontuario.aplicacao.PrescricaoInfo;
import br.edu.ifpa.saudecerta.prontuario.aplicacao.ProntuarioInfo;
import br.edu.ifpa.saudecerta.prontuario.aplicacao.ProntuarioService;
import br.edu.ifpa.saudecerta.prontuario.aplicacao.RegistroClinico;
import br.edu.ifpa.saudecerta.prontuario.dominio.Prescricao.Item;
import br.edu.ifpa.saudecerta.seguranca.api.UsuarioAtual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** ADAPTADOR DE ENTRADA do prontuario. Acesso restrito ao perfil PROFISSIONAL (SecurityConfig). */
@RestController
public class ProntuarioController {

    public record InicioDeAtendimento(@NotNull(message = "Informe a consulta") Long consultaId) {
    }

    public record NovaPrescricao(@NotNull(message = "Informe os medicamentos.") List<Item> itens) {
    }

    private final ProntuarioService prontuario;

    public ProntuarioController(ProntuarioService prontuario) {
        this.prontuario = prontuario;
    }

    @GetMapping("/api/pacientes/{pacienteId}/prontuario")
    public ProntuarioInfo doPaciente(@PathVariable Long pacienteId, @AuthenticationPrincipal Jwt token) {
        return prontuario.doPaciente(pacienteId, UsuarioAtual.de(token));
    }

    @PostMapping("/api/atendimentos")
    public AtendimentoInfo iniciar(@Valid @RequestBody InicioDeAtendimento dados,
            @AuthenticationPrincipal Jwt token) {
        return prontuario.iniciar(dados.consultaId(), UsuarioAtual.de(token));
    }

    @GetMapping("/api/atendimentos/{id}")
    public AtendimentoInfo ler(@PathVariable Long id, @AuthenticationPrincipal Jwt token) {
        return prontuario.ler(id, UsuarioAtual.de(token));
    }

    @PutMapping("/api/atendimentos/{id}")
    public AtendimentoInfo registrar(@PathVariable Long id, @Valid @RequestBody RegistroClinico registro,
            @AuthenticationPrincipal Jwt token) {
        return prontuario.registrar(id, registro, UsuarioAtual.de(token));
    }

    @PostMapping("/api/atendimentos/{id}/finalizacao")
    public AtendimentoInfo finalizar(@PathVariable Long id, @AuthenticationPrincipal Jwt token) {
        return prontuario.finalizar(id, UsuarioAtual.de(token));
    }

    @GetMapping("/api/atendimentos/{id}/prescricoes")
    public List<PrescricaoInfo> prescricoes(@PathVariable Long id, @AuthenticationPrincipal Jwt token) {
        return prontuario.lerPrescricoes(id, UsuarioAtual.de(token));
    }

    @PostMapping("/api/atendimentos/{id}/prescricoes")
    @ResponseStatus(HttpStatus.CREATED)
    public PrescricaoInfo prescrever(@PathVariable Long id, @Valid @RequestBody NovaPrescricao dados,
            @AuthenticationPrincipal Jwt token) {
        return prontuario.prescrever(id, dados.itens(), UsuarioAtual.de(token));
    }
}
