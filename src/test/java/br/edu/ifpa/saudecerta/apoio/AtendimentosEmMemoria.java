package br.edu.ifpa.saudecerta.apoio;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import br.edu.ifpa.saudecerta.prontuario.aplicacao.AtendimentoRepositorio;
import br.edu.ifpa.saudecerta.prontuario.dominio.Atendimento;

public class AtendimentosEmMemoria implements AtendimentoRepositorio {

    private final Map<Long, Atendimento> atendimentos = new HashMap<>();
    private long ultimoId;

    @Override
    public Atendimento salvar(Atendimento atendimento) {
        Long id = atendimento.getId() == null ? ++ultimoId : atendimento.getId();
        atendimentos.put(id, copia(id, atendimento));
        return copia(id, atendimento);
    }

    @Override
    public Optional<Atendimento> buscarPorId(Long id) {
        return Optional.ofNullable(atendimentos.get(id)).map(a -> copia(id, a));
    }

    @Override
    public Optional<Atendimento> buscarParaAlterar(Long id) {
        return buscarPorId(id);
    }

    @Override
    public Optional<Atendimento> buscarPorConsulta(Long consultaId) {
        return atendimentos.values().stream().filter(a -> a.getConsultaId().equals(consultaId)).findFirst()
                .map(a -> copia(a.getId(), a));
    }

    @Override
    public List<Atendimento> doPaciente(Long pacienteId) {
        return atendimentos.values().stream().filter(a -> a.getPacienteId().equals(pacienteId))
                .sorted(Comparator.comparing(Atendimento::getInicio).reversed())
                .map(a -> copia(a.getId(), a)).toList();
    }

    private static Atendimento copia(Long id, Atendimento a) {
        return Atendimento.reconstituir(id, a.getConsultaId(), a.getPacienteId(), a.getProfissionalId(),
                a.getInicio(), a.getFim(), a.getQueixaPrincipal(), a.getEvolucao(), a.getConduta(), a.getStatus());
    }
}
