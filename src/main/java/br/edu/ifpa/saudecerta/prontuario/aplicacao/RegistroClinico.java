package br.edu.ifpa.saudecerta.prontuario.aplicacao;

import jakarta.validation.constraints.Size;

/** Texto clinico digitado pelo profissional durante o atendimento. */
public record RegistroClinico(
        @Size(max = 300, message = "Queixa com no máximo 300 caracteres") String queixaPrincipal,
        @Size(max = 4000, message = "Evolução com no máximo 4000 caracteres") String evolucao,
        @Size(max = 2000, message = "Conduta com no máximo 2000 caracteres") String conduta) {
}
