package br.edu.ifpa.saudecerta.auditoria.api;

/** Porta que os outros modulos usam para deixar a trilha de auditoria. */
public interface Auditoria {

    void registrar(EventoDeAuditoria evento);
}
