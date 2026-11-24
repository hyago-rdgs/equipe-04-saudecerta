package br.edu.ifpa.saudecerta.prontuario.dominio;

import java.time.LocalDateTime;

import br.edu.ifpa.saudecerta.compartilhado.erro.AcessoNegadoException;
import br.edu.ifpa.saudecerta.compartilhado.erro.RegraDeNegocioException;

/**
 * Registro clinico de uma consulta. Classe de DOMINIO (Java puro).
 *
 * Regras:
 *  - so o profissional responsavel escreve no atendimento;
 *  - depois de finalizado, o registro e imutavel (documento clinico);
 *  - nao se finaliza um atendimento sem evolucao registrada.
 */
public class Atendimento {

    private final Long id;
    private final Long consultaId;
    private final Long pacienteId;
    private final Long profissionalId;
    private final LocalDateTime inicio;
    private LocalDateTime fim;
    private String queixaPrincipal;
    private String evolucao;
    private String conduta;
    private StatusAtendimento status;

    private Atendimento(Long id, Long consultaId, Long pacienteId, Long profissionalId, LocalDateTime inicio,
            LocalDateTime fim, String queixaPrincipal, String evolucao, String conduta, StatusAtendimento status) {
        this.id = id;
        this.consultaId = consultaId;
        this.pacienteId = pacienteId;
        this.profissionalId = profissionalId;
        this.inicio = inicio;
        this.fim = fim;
        this.queixaPrincipal = queixaPrincipal;
        this.evolucao = evolucao;
        this.conduta = conduta;
        this.status = status;
    }

    public static Atendimento iniciar(Long consultaId, Long pacienteId, Long profissionalId, LocalDateTime agora) {
        return new Atendimento(null, consultaId, pacienteId, profissionalId, agora, null, null, null, null,
                StatusAtendimento.ABERTO);
    }

    /** Recria um atendimento ja existente (uso exclusivo da persistencia). */
    public static Atendimento reconstituir(Long id, Long consultaId, Long pacienteId, Long profissionalId,
            LocalDateTime inicio, LocalDateTime fim, String queixaPrincipal, String evolucao, String conduta,
            StatusAtendimento status) {
        return new Atendimento(id, consultaId, pacienteId, profissionalId, inicio, fim, queixaPrincipal, evolucao,
                conduta, status);
    }

    public void exigirResponsavel(Long profissionalId) {
        if (profissionalId == null || !profissionalId.equals(this.profissionalId)) {
            throw new AcessoNegadoException("Somente o profissional responsável pode alterar este atendimento.");
        }
    }

    public void registrar(String queixaPrincipal, String evolucao, String conduta) {
        exigirAberto();
        this.queixaPrincipal = limpar(queixaPrincipal);
        this.evolucao = limpar(evolucao);
        this.conduta = limpar(conduta);
    }

    public void finalizar(LocalDateTime agora) {
        exigirAberto();
        if (evolucao == null) {
            throw new RegraDeNegocioException("Registre a evolução clínica antes de finalizar o atendimento.");
        }
        this.status = StatusAtendimento.FINALIZADO;
        this.fim = agora;
    }

    public boolean aberto() {
        return status == StatusAtendimento.ABERTO;
    }

    private void exigirAberto() {
        if (!aberto()) {
            throw new RegraDeNegocioException("Atendimento finalizado não pode ser alterado.");
        }
    }

    private static String limpar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    public Long getId() { return id; }
    public Long getConsultaId() { return consultaId; }
    public Long getPacienteId() { return pacienteId; }
    public Long getProfissionalId() { return profissionalId; }
    public LocalDateTime getInicio() { return inicio; }
    public LocalDateTime getFim() { return fim; }
    public String getQueixaPrincipal() { return queixaPrincipal; }
    public String getEvolucao() { return evolucao; }
    public String getConduta() { return conduta; }
    public StatusAtendimento getStatus() { return status; }
}
