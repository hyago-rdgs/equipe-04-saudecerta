package br.edu.ifpa.saudecerta.cadastro.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "profissional")
public class Profissional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    /** Registro no conselho de classe (ex.: CRM-PA 12345). */
    @Column(nullable = false, unique = true, length = 30)
    private String registro;

    @ManyToOne(optional = false)
    @JoinColumn(name = "especialidade_id")
    private Especialidade especialidade;

    @Column(nullable = false)
    private boolean ativo = true;

    protected Profissional() {
    }

    public Profissional(String nome, String registro, Especialidade especialidade) {
        this.nome = nome;
        this.registro = registro;
        this.especialidade = especialidade;
    }

    public void atualizar(String nome, String registro, Especialidade especialidade, boolean ativo) {
        this.nome = nome;
        this.registro = registro;
        this.especialidade = especialidade;
        this.ativo = ativo;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getRegistro() { return registro; }
    public Especialidade getEspecialidade() { return especialidade; }
    public boolean isAtivo() { return ativo; }
}
