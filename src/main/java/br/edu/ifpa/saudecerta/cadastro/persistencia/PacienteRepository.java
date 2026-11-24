package br.edu.ifpa.saudecerta.cadastro.persistencia;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.ifpa.saudecerta.cadastro.modelo.Paciente;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    /** Busca por parte do nome (sem diferenciar maiusculas) ou por parte do CPF. */
    @Query("""
            select p from Paciente p
            where p.ativo = true
              and (lower(p.nome) like lower(concat('%', :termo, '%'))
                   or (:digitos <> '' and p.cpf like concat('%', :digitos, '%')))
            order by p.nome
            """)
    List<Paciente> buscar(@Param("termo") String termo, @Param("digitos") String digitos);

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);

    long countByAtivoTrue();
}
