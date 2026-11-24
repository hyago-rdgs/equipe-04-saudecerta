package br.edu.ifpa.saudecerta.cadastro.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "especialidade")
public class Especialidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String nome;

    protected Especialidade() {
    }

    public Especialidade(String nome) {
        this.nome = nome;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
}
