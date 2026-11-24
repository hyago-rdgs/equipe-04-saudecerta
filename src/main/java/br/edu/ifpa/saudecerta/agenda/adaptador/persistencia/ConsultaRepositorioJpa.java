package br.edu.ifpa.saudecerta.agenda.adaptador.persistencia;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import br.edu.ifpa.saudecerta.agenda.aplicacao.ConsultaRepositorio;
import br.edu.ifpa.saudecerta.agenda.dominio.Consulta;

/** ADAPTADOR DE SAIDA: implementa a porta ConsultaRepositorio usando Spring Data JPA. */
@Component
public class ConsultaRepositorioJpa implements ConsultaRepositorio {

    private final ConsultaJpaRepository jpa;

    public ConsultaRepositorioJpa(ConsultaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Consulta salvar(Consulta consulta) {
        return jpa.save(ConsultaJpa.de(consulta)).paraDominio();
    }

    @Override
    public Optional<Consulta> buscarPorId(Long id) {
        return jpa.findById(id).map(ConsultaJpa::paraDominio);
    }

    @Override
    public List<Consulta> doDia(LocalDate data) {
        return jpa.findByInicioBetweenOrderByInicio(data.atStartOfDay(), data.atTime(LocalTime.MAX)).stream()
                .map(ConsultaJpa::paraDominio).toList();
    }

    @Override
    public List<Consulta> doProfissionalNoDia(Long profissionalId, LocalDate data) {
        return jpa.findByProfissionalIdAndInicioBetweenOrderByInicio(profissionalId, data.atStartOfDay(),
                data.atTime(LocalTime.MAX)).stream().map(ConsultaJpa::paraDominio).toList();
    }

    @Override
    public List<Consulta> doPacienteNoDia(Long pacienteId, LocalDate data) {
        return jpa.findByPacienteIdAndInicioBetweenOrderByInicio(pacienteId, data.atStartOfDay(),
                data.atTime(LocalTime.MAX)).stream().map(ConsultaJpa::paraDominio).toList();
    }
}
