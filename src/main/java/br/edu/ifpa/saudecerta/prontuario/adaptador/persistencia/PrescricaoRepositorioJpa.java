package br.edu.ifpa.saudecerta.prontuario.adaptador.persistencia;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import br.edu.ifpa.saudecerta.prontuario.aplicacao.PrescricaoInfo;
import br.edu.ifpa.saudecerta.prontuario.aplicacao.PrescricaoRepositorio;
import br.edu.ifpa.saudecerta.prontuario.dominio.Prescricao;

@Component
public class PrescricaoRepositorioJpa implements PrescricaoRepositorio {

    private final PrescricaoJpaRepository jpa;

    public PrescricaoRepositorioJpa(PrescricaoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public PrescricaoInfo salvar(Prescricao prescricao, LocalDateTime emitidaEm) {
        return jpa.save(new PrescricaoJpa(prescricao, emitidaEm)).paraLeitura();
    }

    @Override
    public Map<Long, List<PrescricaoInfo>> dosAtendimentos(Collection<Long> atendimentoIds) {
        if (atendimentoIds.isEmpty()) {
            return Map.of();
        }
        return jpa.dosAtendimentos(atendimentoIds).stream().collect(Collectors.groupingBy(
                PrescricaoJpa::atendimentoId, Collectors.mapping(PrescricaoJpa::paraLeitura, Collectors.toList())));
    }
}
