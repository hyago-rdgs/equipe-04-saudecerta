package br.edu.ifpa.saudecerta.agenda.adaptador.persistencia;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultaJpaRepository extends JpaRepository<ConsultaJpa, Long> {

    List<ConsultaJpa> findByInicioBetweenOrderByInicio(LocalDateTime de, LocalDateTime ate);

    List<ConsultaJpa> findByProfissionalIdAndInicioBetweenOrderByInicio(Long profissionalId, LocalDateTime de,
            LocalDateTime ate);

    List<ConsultaJpa> findByPacienteIdAndInicioBetweenOrderByInicio(Long pacienteId, LocalDateTime de,
            LocalDateTime ate);
}
