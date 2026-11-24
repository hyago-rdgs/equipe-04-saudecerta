package br.edu.ifpa.saudecerta.agenda.aplicacao;

import java.time.LocalDateTime;

/** Dados de agendamento que o paciente pode ver no portal. */
public record ConsultaDoPaciente(Long id, LocalDateTime inicio, int duracaoMinutos, String status, String profissional,
        String especialidade, boolean futura, boolean cancelavel) {
}
