package br.edu.ifpa.saudecerta.painel.aplicacao;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record RelatorioDeConsultas(LocalDate de, LocalDate ate, long total, Map<String, Long> porSituacao,
        List<TotalPorProfissional> porProfissional, Map<String, Long> porEspecialidade) {

    public record TotalPorProfissional(Long profissionalId, String nome, long total) {
    }
}
