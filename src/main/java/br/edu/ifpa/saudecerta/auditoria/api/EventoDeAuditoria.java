package br.edu.ifpa.saudecerta.auditoria.api;

/**
 * Quem fez o que, em qual recurso. O "quando" e carimbado pelo modulo de auditoria.
 *
 * @param pacienteId paciente cujos dados foram tocados (quando houver); permite
 *                   responder "quem acessou o prontuario desta pessoa?".
 */
public record EventoDeAuditoria(String usuario, String perfil, String acao, String recurso, Long recursoId,
        Long pacienteId, String detalhe) {
}
