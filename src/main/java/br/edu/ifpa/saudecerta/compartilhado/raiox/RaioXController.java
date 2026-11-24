package br.edu.ifpa.saudecerta.compartilhado.raiox;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifpa.saudecerta.compartilhado.erro.RecursoNaoEncontradoException;

@RestController
@RequestMapping("/api/raio-x")
public class RaioXController {

    private final RaioXArmazem armazem;

    public RaioXController(RaioXArmazem armazem) {
        this.armazem = armazem;
    }

    @GetMapping
    public List<Rastro> recentes() {
        return armazem.recentes();
    }

    @GetMapping("/{id}")
    public Rastro buscar(@PathVariable String id) {
        return armazem.buscar(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Rastro " + id + " não está mais em memória."));
    }
}
