package br.edu.ifpa.saudecerta.apoio;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import br.edu.ifpa.saudecerta.agenda.api.TotalDeConsultas;
import br.edu.ifpa.saudecerta.agenda.aplicacao.ConsultaRepositorio;
import br.edu.ifpa.saudecerta.agenda.dominio.Consulta;

public class ConsultasEmMemoria implements ConsultaRepositorio {

    private final Map<Long, Consulta> consultas = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();

    @Override
    public Consulta salvar(Consulta consulta) {
        Long id = consulta.getId() == null ? ids.incrementAndGet() : consulta.getId();
        consultas.put(id, copia(id, consulta));
        return copia(id, consulta);
    }

    @Override
    public Optional<Consulta> buscarPorId(Long id) {
        return Optional.ofNullable(consultas.get(id)).map(c -> copia(id, c));
    }

    @Override
    public Optional<Consulta> buscarParaAlterar(Long id) {
        return buscarPorId(id);
    }

    @Override
    public Optional<Consulta> buscarDoPacienteParaAlterar(Long id, Long pacienteId) {
        return buscarPorId(id).filter(c -> c.getPacienteId().equals(pacienteId));
    }

    @Override
    public List<Consulta> doPaciente(Long pacienteId) {
        return consultas.values().stream().filter(c -> c.getPacienteId().equals(pacienteId))
                .sorted(Comparator.comparing(Consulta::getInicio).thenComparing(Consulta::getId).reversed())
                .map(c -> copia(c.getId(), c)).toList();
    }

    @Override
    public List<Consulta> doDia(LocalDate data) {
        return consultas.values().stream().filter(c -> c.getInicio().toLocalDate().equals(data))
                .sorted(Comparator.comparing(Consulta::getInicio))
                .map(c -> copia(c.getId(), c)).toList();
    }

    @Override
    public List<Consulta> doProfissionalNoDia(Long profissionalId, LocalDate data) {
        return doDia(data).stream().filter(c -> c.getProfissionalId().equals(profissionalId)).toList();
    }

    @Override
    public List<Consulta> doPacienteNoDia(Long pacienteId, LocalDate data) {
        return doDia(data).stream().filter(c -> c.getPacienteId().equals(pacienteId)).toList();
    }

    @Override
    public List<TotalDeConsultas> totaisPorProfissionalESituacao(LocalDate de, LocalDate ate) {
        record Grupo(Long profissionalId, String situacao) {}
        return consultas.values().stream()
                .filter(c -> !c.getInicio().toLocalDate().isBefore(de) && !c.getInicio().toLocalDate().isAfter(ate))
                .collect(Collectors.groupingBy(c -> new Grupo(c.getProfissionalId(), c.getStatus().name()),
                        Collectors.counting()))
                .entrySet().stream()
                .map(e -> new TotalDeConsultas(e.getKey().profissionalId(), e.getKey().situacao(), e.getValue()))
                .toList();
    }

    private static Consulta copia(Long id, Consulta c) {
        return Consulta.reconstituir(id, c.getPacienteId(), c.getProfissionalId(), c.getInicio(),
                c.getDuracaoMinutos(), c.getStatus(), c.getObservacao(), c.getMotivoCancelamento());
    }
}
