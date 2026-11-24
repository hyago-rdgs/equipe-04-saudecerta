package br.edu.ifpa.saudecerta.cadastro.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifpa.saudecerta.cadastro.aplicacao.ProfissionalDados;
import br.edu.ifpa.saudecerta.cadastro.aplicacao.ProfissionalService;
import br.edu.ifpa.saudecerta.cadastro.modelo.Especialidade;
import br.edu.ifpa.saudecerta.cadastro.modelo.Profissional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
public class ProfissionalController {

    public record EspecialidadeDados(
            @NotBlank(message = "Informe o nome") @Size(max = 80, message = "Nome com no máximo 80 caracteres") String nome) {
    }

    public record EspecialidadeResposta(Long id, String nome) {
    }

    private final ProfissionalService profissionais;

    public ProfissionalController(ProfissionalService profissionais) {
        this.profissionais = profissionais;
    }

    @GetMapping("/api/profissionais")
    public List<ProfissionalResposta> listar() {
        return profissionais.listar().stream().map(ProfissionalResposta::de).toList();
    }

    @PostMapping("/api/profissionais")
    public ResponseEntity<ProfissionalResposta> criar(@Valid @RequestBody ProfissionalDados dados) {
        Profissional criado = profissionais.criar(dados);
        return ResponseEntity.created(URI.create("/api/profissionais/" + criado.getId()))
                .body(ProfissionalResposta.de(criado));
    }

    @PutMapping("/api/profissionais/{id}")
    public ProfissionalResposta alterar(@PathVariable Long id, @Valid @RequestBody ProfissionalDados dados) {
        return ProfissionalResposta.de(profissionais.alterar(id, dados));
    }

    @GetMapping("/api/especialidades")
    public List<EspecialidadeResposta> especialidades() {
        return profissionais.listarEspecialidades().stream()
                .map(e -> new EspecialidadeResposta(e.getId(), e.getNome())).toList();
    }

    @PostMapping("/api/especialidades")
    public ResponseEntity<EspecialidadeResposta> criarEspecialidade(@Valid @RequestBody EspecialidadeDados dados) {
        Especialidade criada = profissionais.criarEspecialidade(dados.nome());
        return ResponseEntity.created(URI.create("/api/especialidades/" + criada.getId()))
                .body(new EspecialidadeResposta(criada.getId(), criada.getNome()));
    }
}
