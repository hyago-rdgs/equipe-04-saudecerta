package br.edu.ifpa.saudecerta.auditoria.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifpa.saudecerta.auditoria.aplicacao.AuditoriaService;
import br.edu.ifpa.saudecerta.auditoria.modelo.RegistroDeAuditoria;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoria;

    public AuditoriaController(AuditoriaService auditoria) {
        this.auditoria = auditoria;
    }

    /** Ultimos 200 registros; com pacienteId, apenas os que tocam aquele paciente. */
    @GetMapping
    public List<RegistroDeAuditoria> listar(@RequestParam(required = false) Long pacienteId) {
        return auditoria.recentes(pacienteId);
    }
}
