/**
 * Modulo AGENDA - marcacao de consultas.
 *
 * Estilo: arquitetura hexagonal (portas e adaptadores), ADR 0002.
 *
 *   dominio/                 regras puras (nenhum import de Spring ou JPA)
 *   aplicacao/               casos de uso + portas de saida (interfaces)
 *   adaptador/web/           entrada: API REST
 *   adaptador/persistencia/  saida: JPA, implementa as portas
 *   api/                     o que outros modulos podem usar
 *
 * As dependencias apontam sempre para dentro: adaptador -> aplicacao -> dominio.
 * O teste ArquiteturaTest reprova o build se alguem inverter essa direcao.
 */
package br.edu.ifpa.saudecerta.agenda;
