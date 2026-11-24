package br.edu.ifpa.saudecerta.agenda.api;

/** Contagem de consultas para um profissional e uma situação. */
public record TotalDeConsultas(Long profissionalId, String situacao, long total) {
}
