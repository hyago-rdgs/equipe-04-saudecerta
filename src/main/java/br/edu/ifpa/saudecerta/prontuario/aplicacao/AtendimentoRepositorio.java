package br.edu.ifpa.saudecerta.prontuario.aplicacao;

import java.util.List;
import java.util.Optional;

import br.edu.ifpa.saudecerta.prontuario.dominio.Atendimento;

/** PORTA DE SAIDA do prontuario. */
public interface AtendimentoRepositorio {

    Atendimento salvar(Atendimento atendimento);

    Optional<Atendimento> buscarPorId(Long id);

    Optional<Atendimento> buscarPorConsulta(Long consultaId);

    /** Historico do paciente, do mais recente para o mais antigo. */
    List<Atendimento> doPaciente(Long pacienteId);
}
