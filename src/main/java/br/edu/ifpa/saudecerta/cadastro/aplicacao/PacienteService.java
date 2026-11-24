package br.edu.ifpa.saudecerta.cadastro.aplicacao;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifpa.saudecerta.auditoria.api.Auditoria;
import br.edu.ifpa.saudecerta.auditoria.api.EventoDeAuditoria;
import br.edu.ifpa.saudecerta.cadastro.modelo.Cpf;
import br.edu.ifpa.saudecerta.cadastro.modelo.Paciente;
import br.edu.ifpa.saudecerta.cadastro.persistencia.PacienteRepository;
import br.edu.ifpa.saudecerta.compartilhado.erro.ConflitoException;
import br.edu.ifpa.saudecerta.compartilhado.erro.RecursoNaoEncontradoException;
import br.edu.ifpa.saudecerta.compartilhado.erro.RegraDeNegocioException;
import br.edu.ifpa.saudecerta.seguranca.api.UsuarioLogado;

@Service
@Transactional
public class PacienteService {

    private final PacienteRepository pacientes;
    private final Auditoria auditoria;

    public PacienteService(PacienteRepository pacientes, Auditoria auditoria) {
        this.pacientes = pacientes;
        this.auditoria = auditoria;
    }

    public List<Paciente> buscar(String termo) {
        String texto = termo == null ? "" : termo.trim();
        return pacientes.buscar(texto, Cpf.limpar(texto));
    }

    public Paciente obter(Long id) {
        return pacientes.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paciente " + id + " não encontrado."));
    }

    public Paciente criar(PacienteDados dados, UsuarioLogado quem) {
        String cpf = cpfValido(dados.cpf());
        if (pacientes.existsByCpf(cpf)) {
            throw new ConflitoException("Já existe um paciente com o CPF " + Cpf.formatar(cpf) + ".");
        }
        Paciente paciente = pacientes.save(new Paciente(dados.nome().trim(), cpf, dados.dataNascimento(),
                vazioComoNulo(dados.telefone()), vazioComoNulo(dados.email()), vazioComoNulo(dados.alertas())));
        auditar(quem, "PACIENTE_CADASTRADO", paciente, "Cadastro criado");
        return paciente;
    }

    public Paciente alterar(Long id, PacienteDados dados, UsuarioLogado quem) {
        Paciente paciente = obter(id);
        String cpf = cpfValido(dados.cpf());
        if (pacientes.existsByCpfAndIdNot(cpf, id)) {
            throw new ConflitoException("Outro paciente já usa o CPF " + Cpf.formatar(cpf) + ".");
        }
        paciente.atualizar(dados.nome().trim(), cpf, dados.dataNascimento(), vazioComoNulo(dados.telefone()),
                vazioComoNulo(dados.email()), vazioComoNulo(dados.alertas()));
        auditar(quem, "PACIENTE_ALTERADO", paciente, "Cadastro alterado");
        return paciente;
    }

    /** Paciente nunca e apagado: o historico clinico precisa continuar existindo. */
    public void inativar(Long id, UsuarioLogado quem) {
        Paciente paciente = obter(id);
        paciente.inativar();
        auditar(quem, "PACIENTE_INATIVADO", paciente, "Cadastro inativado");
    }

    private static String cpfValido(String informado) {
        if (!Cpf.valido(informado)) {
            throw new RegraDeNegocioException("CPF inválido. Confira os dígitos.");
        }
        return Cpf.limpar(informado);
    }

    private static String vazioComoNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private void auditar(UsuarioLogado quem, String acao, Paciente paciente, String detalhe) {
        auditoria.registrar(new EventoDeAuditoria(quem.login(), quem.perfil().name(), acao, "Paciente",
                paciente.getId(), paciente.getId(), detalhe));
    }
}
