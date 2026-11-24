package br.edu.ifpa.saudecerta.agenda.adaptador.persistencia;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import br.edu.ifpa.saudecerta.agenda.dominio.StatusConsulta;
import jakarta.persistence.LockModeType;

public interface ConsultaJpaRepository extends JpaRepository<ConsultaJpa, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConsultaJpa c where c.id = :id")
    Optional<ConsultaJpa> buscarParaAlterar(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ConsultaJpa> findByIdAndPacienteId(Long id, Long pacienteId);

    List<ConsultaJpa> findByPacienteIdOrderByInicioDescIdDesc(Long pacienteId);

    interface ContagemPorProfissionalESituacao {
        Long getProfissionalId();

        StatusConsulta getStatus();

        long getTotal();
    }

    @Query("""
            select c.profissionalId as profissionalId, c.status as status, count(c) as total
            from ConsultaJpa c
            where c.inicio >= :inicio and c.inicio < :fimExclusivo
            group by c.profissionalId, c.status
            """)
    List<ContagemPorProfissionalESituacao> contarPorProfissionalESituacao(LocalDateTime inicio,
            LocalDateTime fimExclusivo);

    List<ConsultaJpa> findByInicioBetweenOrderByInicio(LocalDateTime de, LocalDateTime ate);

    List<ConsultaJpa> findByProfissionalIdAndInicioBetweenOrderByInicio(Long profissionalId, LocalDateTime de,
            LocalDateTime ate);

    List<ConsultaJpa> findByPacienteIdAndInicioBetweenOrderByInicio(Long pacienteId, LocalDateTime de,
            LocalDateTime ate);
}
