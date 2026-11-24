package br.edu.ifpa.saudecerta.cadastro.modelo;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "paciente")
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    /** Somente digitos. */
    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(nullable = false)
    private LocalDate dataNascimento;

    @Column(length = 20)
    private String telefone;

    @Column(length = 120)
    private String email;

    /** Alergias e avisos que a equipe precisa ver antes de atender. */
    @Column(length = 300)
    private String alertas;

    @Column(nullable = false)
    private boolean ativo = true;

    protected Paciente() {
    }

    public Paciente(String nome, String cpf, LocalDate dataNascimento, String telefone, String email,
            String alertas) {
        atualizar(nome, cpf, dataNascimento, telefone, email, alertas);
    }

    public void atualizar(String nome, String cpf, LocalDate dataNascimento, String telefone, String email,
            String alertas) {
        this.nome = nome;
        this.cpf = cpf;
        this.dataNascimento = dataNascimento;
        this.telefone = telefone;
        this.email = email;
        this.alertas = alertas;
    }

    public void inativar() {
        this.ativo = false;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getCpf() { return cpf; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public String getTelefone() { return telefone; }
    public String getEmail() { return email; }
    public String getAlertas() { return alertas; }
    public boolean isAtivo() { return ativo; }
}
