package br.edu.ifpa.saudecerta.cadastro.aplicacao;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/** Dados aceitos ao criar ou alterar um paciente. */
public record PacienteDados(
        @NotBlank(message = "Informe o nome") @Size(max = 120, message = "Nome com no máximo 120 caracteres") String nome,
        @NotBlank(message = "Informe o CPF") String cpf,
        @NotNull(message = "Informe a data de nascimento") @Past(message = "A data de nascimento deve estar no passado") LocalDate dataNascimento,
        @Size(max = 20, message = "Telefone com no máximo 20 caracteres") String telefone,
        @Email(message = "E-mail inválido") @Size(max = 120, message = "E-mail com no máximo 120 caracteres") String email,
        @Size(max = 300, message = "Alertas com no máximo 300 caracteres") String alertas) {
}
