package br.edu.ifpa.saudecerta.seguranca.modelo;

import br.edu.ifpa.saudecerta.seguranca.api.Perfil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String login;

    @Column(nullable = false, length = 120)
    private String nome;

    /** Nunca a senha em claro: apenas o hash BCrypt. */
    @Column(nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Perfil perfil;

    /** Preenchido quando o usuario e um profissional de saude. */
    private Long profissionalId;

    /** Preenchido quando o usuario e um paciente. */
    private Long pacienteId;

    protected Usuario() {
    }

    public Usuario(String login, String nome, String senhaHash, Perfil perfil, Long profissionalId, Long pacienteId) {
        this.login = login;
        this.nome = nome;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.profissionalId = profissionalId;
        this.pacienteId = pacienteId;
    }

    public Long getId() { return id; }
    public String getLogin() { return login; }
    public String getNome() { return nome; }
    public String getSenhaHash() { return senhaHash; }
    public Perfil getPerfil() { return perfil; }
    public Long getProfissionalId() { return profissionalId; }
    public Long getPacienteId() { return pacienteId; }
}
