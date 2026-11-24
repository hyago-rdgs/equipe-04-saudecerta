package br.edu.ifpa.saudecerta.prontuario.adaptador.persistencia;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PrescricaoJpaRepository extends JpaRepository<PrescricaoJpa, Long> {

    @Query("""
            select distinct p from PrescricaoJpa p left join fetch p.itens
            where p.atendimentoId in :atendimentoIds order by p.id
            """)
    List<PrescricaoJpa> dosAtendimentos(Collection<Long> atendimentoIds);
}
