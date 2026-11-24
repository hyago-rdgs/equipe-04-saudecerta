package br.edu.ifpa.saudecerta.painel.dominio;

import java.time.LocalDate;

import br.edu.ifpa.saudecerta.compartilhado.erro.RegraDeNegocioException;

/** Intervalo inclusivo de até um ano civil. */
public record PeriodoDoRelatorio(LocalDate de, LocalDate ate) {

    public PeriodoDoRelatorio {
        if (ate.isBefore(de)) {
            throw new RegraDeNegocioException("A data final não pode ser anterior à data inicial.");
        }
        if (ate.getYear() > de.getYear() && ate.isAfter(de.plusYears(1))) {
            throw new RegraDeNegocioException("O período do relatório não pode ultrapassar um ano.");
        }
    }
}
