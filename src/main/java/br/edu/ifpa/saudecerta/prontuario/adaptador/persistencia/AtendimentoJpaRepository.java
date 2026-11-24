package br.edu.ifpa.saudecerta.prontuario.adaptador.persistencia;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface AtendimentoJpaRepository extends JpaRepository<AtendimentoJpa, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AtendimentoJpa a where a.id = :id")
    Optional<AtendimentoJpa> buscarParaAlterar(Long id);

    Optional<AtendimentoJpa> findByConsultaId(Long consultaId);

    List<AtendimentoJpa> findByPacienteIdOrderByInicioDesc(Long pacienteId);
}
