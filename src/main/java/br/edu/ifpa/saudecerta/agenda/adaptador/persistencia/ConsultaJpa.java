package br.edu.ifpa.saudecerta.agenda.adaptador.persistencia;

import java.time.LocalDateTime;

import br.edu.ifpa.saudecerta.agenda.dominio.Consulta;
import br.edu.ifpa.saudecerta.agenda.dominio.StatusConsulta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Como a consulta e guardada no banco. E um detalhe do adaptador: o dominio
 * nao conhece esta classe. O preco da separacao e o mapeamento de ida e volta
 * logo abaixo (discuta em aula: quando esse custo compensa?).
 *
 * Paciente e profissional sao guardados so como ID: eles pertencem a outro
 * modulo (cadastro) e nao ha chave estrangeira JPA cruzando a fronteira.
 */
@Entity
@Table(name = "consulta")
public class ConsultaJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long pacienteId;

    @Column(nullable = false)
    private Long profissionalId;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private int duracaoMinutos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusConsulta status;

    @Column(length = 200)
    private String observacao;

    @Column(length = 200)
    private String motivoCancelamento;

    protected ConsultaJpa() {
    }

    static ConsultaJpa de(Consulta c) {
        ConsultaJpa jpa = new ConsultaJpa();
        jpa.id = c.getId();
        jpa.pacienteId = c.getPacienteId();
        jpa.profissionalId = c.getProfissionalId();
        jpa.inicio = c.getInicio();
        jpa.duracaoMinutos = c.getDuracaoMinutos();
        jpa.status = c.getStatus();
        jpa.observacao = c.getObservacao();
        jpa.motivoCancelamento = c.getMotivoCancelamento();
        return jpa;
    }

    Consulta paraDominio() {
        return Consulta.reconstituir(id, pacienteId, profissionalId, inicio, duracaoMinutos, status, observacao,
                motivoCancelamento);
    }
}
