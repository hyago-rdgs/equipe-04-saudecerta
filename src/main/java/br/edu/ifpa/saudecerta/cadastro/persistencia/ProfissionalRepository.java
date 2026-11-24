package br.edu.ifpa.saudecerta.cadastro.persistencia;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import br.edu.ifpa.saudecerta.cadastro.modelo.Profissional;
import jakarta.persistence.LockModeType;

public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Profissional m where m.id = :id")
    Optional<Profissional> buscarParaReserva(Long id);

    List<Profissional> findAllByOrderByNome();

    List<Profissional> findByAtivoTrueOrderByNome();

    boolean existsByRegistro(String registro);

    boolean existsByRegistroAndIdNot(String registro, Long id);
}
