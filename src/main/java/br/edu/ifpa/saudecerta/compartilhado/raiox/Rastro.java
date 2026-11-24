package br.edu.ifpa.saudecerta.compartilhado.raiox;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Tudo o que o Raio-X observou em uma requisicao HTTP. */
public class Rastro {

    private final String id;
    private final String metodo;
    private final String caminho;
    private final LocalDateTime quando = LocalDateTime.now();
    private final List<Passo> passos = new ArrayList<>();
    private int status;
    private double duracaoMs;
    private int profundidadeAtual;

    Rastro(String id, String metodo, String caminho) {
        this.id = id;
        this.metodo = metodo;
        this.caminho = caminho;
    }

    Passo entrar(String camada, String componente, String operacao) {
        Passo passo = new Passo(passos.size() + 1, profundidadeAtual, camada, componente, operacao);
        passos.add(passo);
        profundidadeAtual++;
        return passo;
    }

    void sair(Passo passo, long nanos) {
        profundidadeAtual--;
        passo.concluir(nanos);
    }

    void concluir(int status, long nanos) {
        this.status = status;
        this.duracaoMs = Math.round(nanos / 10_000.0) / 100.0;
    }

    public String getId() { return id; }
    public String getMetodo() { return metodo; }
    public String getCaminho() { return caminho; }
    public LocalDateTime getQuando() { return quando; }
    public List<Passo> getPassos() { return passos; }
    public int getStatus() { return status; }
    public double getDuracaoMs() { return duracaoMs; }
}
