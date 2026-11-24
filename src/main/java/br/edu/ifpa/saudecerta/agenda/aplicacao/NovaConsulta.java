package br.edu.ifpa.saudecerta.agenda.aplicacao;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Comando de entrada do caso de uso "agendar consulta". */
public record NovaConsulta(
        @NotNull(message = "Escolha o paciente") Long pacienteId,
        @NotNull(message = "Escolha o profissional") Long profissionalId,
        @NotNull(message = "Escolha o horário") LocalDateTime inicio,
        @Size(max = 200, message = "Observação com no máximo 200 caracteres") String observacao) {
}
