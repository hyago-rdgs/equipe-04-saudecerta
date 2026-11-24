package br.edu.ifpa.saudecerta.auditoria.modelo;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Registro imutavel: nao ha setters nem operacao de alteracao/exclusao. */
@Entity
@Table(name = "registro_auditoria")
public class RegistroDeAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime quando;

    @Column(length = 60)
    private String usuario;

    @Column(length = 20)
    private String perfil;

    @Column(nullable = false, length = 40)
    private String acao;

    @Column(length = 40)
    private String recurso;

    private Long recursoId;

    private Long pacienteId;

    @Column(length = 300)
    private String detalhe;

    protected RegistroDeAuditoria() {
    }

    public RegistroDeAuditoria(LocalDateTime quando, String usuario, String perfil, String acao, String recurso,
            Long recursoId, Long pacienteId, String detalhe) {
        this.quando = quando;
        this.usuario = usuario;
        this.perfil = perfil;
        this.acao = acao;
        this.recurso = recurso;
        this.recursoId = recursoId;
        this.pacienteId = pacienteId;
        this.detalhe = detalhe;
    }

    public Long getId() { return id; }
    public LocalDateTime getQuando() { return quando; }
    public String getUsuario() { return usuario; }
    public String getPerfil() { return perfil; }
    public String getAcao() { return acao; }
    public String getRecurso() { return recurso; }
    public Long getRecursoId() { return recursoId; }
    public Long getPacienteId() { return pacienteId; }
    public String getDetalhe() { return detalhe; }
}
