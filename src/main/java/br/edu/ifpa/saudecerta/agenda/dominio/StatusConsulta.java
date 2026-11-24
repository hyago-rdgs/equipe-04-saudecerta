package br.edu.ifpa.saudecerta.agenda.dominio;

/**
 * Ciclo de vida de uma consulta:
 *
 *   AGENDADA -> CONFIRMADA -> EM_ATENDIMENTO -> REALIZADA
 *       \____________\______-> CANCELADA
 *
 * CONFIRMADA significa "paciente chegou e esta aguardando".
 */
public enum StatusConsulta {
    AGENDADA,
    CONFIRMADA,
    EM_ATENDIMENTO,
    REALIZADA,
    CANCELADA
}
