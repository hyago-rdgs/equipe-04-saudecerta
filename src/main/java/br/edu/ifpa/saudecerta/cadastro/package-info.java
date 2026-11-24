/**
 * Modulo CADASTRO - pacientes, profissionais e especialidades.
 *
 * Estilo: camadas tradicionais (web -> aplicacao -> persistencia), com as
 * entidades JPA servindo tambem de modelo. E o estilo mais barato e basta
 * para CRUD com pouca regra. Compare com os modulos agenda e prontuario,
 * que usam arquitetura hexagonal (ADR 0002).
 *
 * Outros modulos so podem usar o pacote "api" (CadastroFachada).
 */
package br.edu.ifpa.saudecerta.cadastro;
