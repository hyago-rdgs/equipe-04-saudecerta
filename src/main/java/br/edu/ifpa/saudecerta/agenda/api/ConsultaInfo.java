package br.edu.ifpa.saudecerta.agenda.api;

import java.time.LocalDateTime;

/** Visao de leitura de uma consulta, ja com os nomes de paciente e profissional. */
public record ConsultaInfo(Long id, LocalDateTime inicio, int duracaoMinutos, String status, Long pacienteId,
        String paciente, String alertasDoPaciente, Long profissionalId, String profissional, String especialidade,
        String observacao, String motivoCancelamento) {
}
