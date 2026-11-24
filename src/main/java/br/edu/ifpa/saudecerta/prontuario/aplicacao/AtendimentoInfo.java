package br.edu.ifpa.saudecerta.prontuario.aplicacao;

import java.time.LocalDateTime;
import java.util.List;

/** Visao de leitura de um atendimento. */
public record AtendimentoInfo(Long id, Long consultaId, Long pacienteId, Long profissionalId, String profissional,
        String especialidade, LocalDateTime inicio, LocalDateTime fim, String queixaPrincipal, String evolucao,
        String conduta, String status, List<PrescricaoInfo> prescricoes) {
}
