package br.edu.ifpa.saudecerta.prontuario.aplicacao;

import java.util.List;

import br.edu.ifpa.saudecerta.cadastro.api.PacienteResumo;

/** O prontuario de um paciente: identificacao + historico de atendimentos. */
public record ProntuarioInfo(PacienteResumo paciente, List<AtendimentoInfo> atendimentos) {
}
