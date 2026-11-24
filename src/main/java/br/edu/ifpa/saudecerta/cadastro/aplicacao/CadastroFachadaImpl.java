package br.edu.ifpa.saudecerta.cadastro.aplicacao;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifpa.saudecerta.cadastro.api.CadastroFachada;
import br.edu.ifpa.saudecerta.cadastro.api.PacienteResumo;
import br.edu.ifpa.saudecerta.cadastro.api.ProfissionalResumo;
import br.edu.ifpa.saudecerta.cadastro.modelo.Paciente;
import br.edu.ifpa.saudecerta.cadastro.modelo.Profissional;
import br.edu.ifpa.saudecerta.cadastro.persistencia.PacienteRepository;
import br.edu.ifpa.saudecerta.cadastro.persistencia.ProfissionalRepository;

@Service
@Transactional(readOnly = true)
public class CadastroFachadaImpl implements CadastroFachada {

    private final PacienteRepository pacientes;
    private final ProfissionalRepository profissionais;

    public CadastroFachadaImpl(PacienteRepository pacientes, ProfissionalRepository profissionais) {
        this.pacientes = pacientes;
        this.profissionais = profissionais;
    }

    @Override
    public Optional<PacienteResumo> paciente(Long id) {
        return pacientes.findById(id).map(CadastroFachadaImpl::resumo);
    }

    @Override
    public Map<Long, PacienteResumo> pacientes(Collection<Long> ids) {
        return pacientes.findAllById(ids).stream().map(CadastroFachadaImpl::resumo)
                .collect(Collectors.toMap(PacienteResumo::id, Function.identity()));
    }

    @Override
    public Optional<ProfissionalResumo> profissional(Long id) {
        return profissionais.findById(id).map(CadastroFachadaImpl::resumo);
    }

    @Override
    public Map<Long, ProfissionalResumo> profissionais(Collection<Long> ids) {
        return profissionais.findAllById(ids).stream().map(CadastroFachadaImpl::resumo)
                .collect(Collectors.toMap(ProfissionalResumo::id, Function.identity()));
    }

    @Override
    public List<ProfissionalResumo> profissionaisAtivos() {
        return profissionais.findByAtivoTrueOrderByNome().stream().map(CadastroFachadaImpl::resumo).toList();
    }

    @Override
    public long totalDePacientesAtivos() {
        return pacientes.countByAtivoTrue();
    }

    private static PacienteResumo resumo(Paciente p) {
        return new PacienteResumo(p.getId(), p.getNome(), p.getCpf(), p.getDataNascimento(), p.getAlertas(),
                p.isAtivo());
    }

    private static ProfissionalResumo resumo(Profissional p) {
        return new ProfissionalResumo(p.getId(), p.getNome(), p.getRegistro(), p.getEspecialidade().getNome(),
                p.isAtivo());
    }
}
