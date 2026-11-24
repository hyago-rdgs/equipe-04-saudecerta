package br.edu.ifpa.saudecerta.cadastro.persistencia;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.ifpa.saudecerta.cadastro.modelo.Especialidade;

public interface EspecialidadeRepository extends JpaRepository<Especialidade, Long> {

    List<Especialidade> findAllByOrderByNome();

    boolean existsByNomeIgnoreCase(String nome);
}
