package br.edu.ifpa.saudecerta.seguranca.api;

/** Papeis do controle de acesso (RBAC). Veja docs/adr/0004-controle-de-acesso-rbac.md. */
public enum Perfil {
    ADMIN,
    RECEPCAO,
    PROFISSIONAL,
    PACIENTE
}
