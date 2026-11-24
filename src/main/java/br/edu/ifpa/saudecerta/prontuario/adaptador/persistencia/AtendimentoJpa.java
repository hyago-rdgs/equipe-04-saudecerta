package br.edu.ifpa.saudecerta.prontuario.adaptador.persistencia;

import java.time.LocalDateTime;

import br.edu.ifpa.saudecerta.prontuario.dominio.Atendimento;
import br.edu.ifpa.saudecerta.prontuario.dominio.StatusAtendimento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "atendimento")
public class AtendimentoJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long consultaId;

    @Column(nullable = false)
    private Long pacienteId;

    @Column(nullable = false)
    private Long profissionalId;

    @Column(nullable = false)
    private LocalDateTime inicio;

    private LocalDateTime fim;

    @Column(length = 300)
    private String queixaPrincipal;

    @Column(length = 4000)
    private String evolucao;

    @Column(length = 2000)
    private String conduta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAtendimento status;

    protected AtendimentoJpa() {
    }

    static AtendimentoJpa de(Atendimento a) {
        AtendimentoJpa jpa = new AtendimentoJpa();
        jpa.id = a.getId();
        jpa.consultaId = a.getConsultaId();
        jpa.pacienteId = a.getPacienteId();
        jpa.profissionalId = a.getProfissionalId();
        jpa.inicio = a.getInicio();
        jpa.fim = a.getFim();
        jpa.queixaPrincipal = a.getQueixaPrincipal();
        jpa.evolucao = a.getEvolucao();
        jpa.conduta = a.getConduta();
        jpa.status = a.getStatus();
        return jpa;
    }

    Atendimento paraDominio() {
        return Atendimento.reconstituir(id, consultaId, pacienteId, profissionalId, inicio, fim, queixaPrincipal,
                evolucao, conduta, status);
    }
}
