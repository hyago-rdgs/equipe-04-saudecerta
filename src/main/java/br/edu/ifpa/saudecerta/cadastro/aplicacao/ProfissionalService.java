package br.edu.ifpa.saudecerta.cadastro.aplicacao;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifpa.saudecerta.cadastro.modelo.Especialidade;
import br.edu.ifpa.saudecerta.cadastro.modelo.Profissional;
import br.edu.ifpa.saudecerta.cadastro.persistencia.EspecialidadeRepository;
import br.edu.ifpa.saudecerta.cadastro.persistencia.ProfissionalRepository;
import br.edu.ifpa.saudecerta.compartilhado.erro.ConflitoException;
import br.edu.ifpa.saudecerta.compartilhado.erro.RecursoNaoEncontradoException;

@Service
@Transactional
public class ProfissionalService {

    private final ProfissionalRepository profissionais;
    private final EspecialidadeRepository especialidades;

    public ProfissionalService(ProfissionalRepository profissionais, EspecialidadeRepository especialidades) {
        this.profissionais = profissionais;
        this.especialidades = especialidades;
    }

    public List<Profissional> listar() {
        return profissionais.findAllByOrderByNome();
    }

    public List<Especialidade> listarEspecialidades() {
        return especialidades.findAllByOrderByNome();
    }

    public Especialidade criarEspecialidade(String nome) {
        String limpo = nome.trim();
        if (especialidades.existsByNomeIgnoreCase(limpo)) {
            throw new ConflitoException("A especialidade " + limpo + " já existe.");
        }
        return especialidades.save(new Especialidade(limpo));
    }

    public Profissional criar(ProfissionalDados dados) {
        String registro = dados.registro().trim();
        if (profissionais.existsByRegistro(registro)) {
            throw new ConflitoException("Já existe um profissional com o registro " + registro + ".");
        }
        return profissionais.save(new Profissional(dados.nome().trim(), registro, especialidade(dados)));
    }

    public Profissional alterar(Long id, ProfissionalDados dados) {
        Profissional profissional = profissionais.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional " + id + " não encontrado."));
        String registro = dados.registro().trim();
        if (profissionais.existsByRegistroAndIdNot(registro, id)) {
            throw new ConflitoException("Outro profissional já usa o registro " + registro + ".");
        }
        profissional.atualizar(dados.nome().trim(), registro, especialidade(dados),
                dados.ativo() == null || dados.ativo());
        return profissional;
    }

    private Especialidade especialidade(ProfissionalDados dados) {
        return especialidades.findById(dados.especialidadeId()).orElseThrow(
                () -> new RecursoNaoEncontradoException("Especialidade " + dados.especialidadeId() + " não encontrada."));
    }
}
