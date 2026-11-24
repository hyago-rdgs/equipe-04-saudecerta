package br.edu.ifpa.saudecerta.cadastro.persistencia;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.ifpa.saudecerta.cadastro.modelo.Profissional;

public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {

    List<Profissional> findAllByOrderByNome();

    List<Profissional> findByAtivoTrueOrderByNome();

    boolean existsByRegistro(String registro);

    boolean existsByRegistroAndIdNot(String registro, Long id);
}
