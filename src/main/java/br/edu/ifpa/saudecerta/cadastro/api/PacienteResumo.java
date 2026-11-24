package br.edu.ifpa.saudecerta.cadastro.api;

import java.time.LocalDate;

public record PacienteResumo(Long id, String nome, String cpf, LocalDate dataNascimento, String alertas,
        boolean ativo) {
}
