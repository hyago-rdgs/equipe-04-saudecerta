package br.edu.ifpa.saudecerta.prontuario.aplicacao;

import java.time.LocalDateTime;

/** Visao de leitura de um atendimento. */
public record AtendimentoInfo(Long id, Long consultaId, Long pacienteId, Long profissionalId, String profissional,
        String especialidade, LocalDateTime inicio, LocalDateTime fim, String queixaPrincipal, String evolucao,
        String conduta, String status) {
}
