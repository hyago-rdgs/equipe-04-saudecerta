package br.edu.ifpa.saudecerta.cadastro.aplicacao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Dados aceitos ao criar ou alterar um profissional. */
public record ProfissionalDados(
        @NotBlank(message = "Informe o nome") @Size(max = 120, message = "Nome com no máximo 120 caracteres") String nome,
        @NotBlank(message = "Informe o registro profissional") @Size(max = 30, message = "Registro com no máximo 30 caracteres") String registro,
        @NotNull(message = "Escolha a especialidade") Long especialidadeId,
        Boolean ativo) {
}
