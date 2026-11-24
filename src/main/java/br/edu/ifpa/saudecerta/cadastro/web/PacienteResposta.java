package br.edu.ifpa.saudecerta.cadastro.web;

import java.time.LocalDate;

import br.edu.ifpa.saudecerta.cadastro.modelo.Cpf;
import br.edu.ifpa.saudecerta.cadastro.modelo.Paciente;

/** DTO de saida: a entidade JPA nao atravessa a fronteira da API. */
public record PacienteResposta(Long id, String nome, String cpf, LocalDate dataNascimento, String telefone,
        String email, String alertas) {

    public static PacienteResposta de(Paciente p) {
        return new PacienteResposta(p.getId(), p.getNome(), Cpf.formatar(p.getCpf()), p.getDataNascimento(),
                p.getTelefone(), p.getEmail(), p.getAlertas());
    }
}
