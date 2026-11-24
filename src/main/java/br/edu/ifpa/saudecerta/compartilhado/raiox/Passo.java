package br.edu.ifpa.saudecerta.compartilhado.raiox;

/** Uma chamada de metodo observada durante a requisicao. */
public class Passo {

    private final int ordem;
    private final int profundidade;
    private final String camada;
    private final String componente;
    private final String operacao;
    private double duracaoMs;

    Passo(int ordem, int profundidade, String camada, String componente, String operacao) {
        this.ordem = ordem;
        this.profundidade = profundidade;
        this.camada = camada;
        this.componente = componente;
        this.operacao = operacao;
    }

    void concluir(long nanos) {
        this.duracaoMs = Math.round(nanos / 10_000.0) / 100.0;
    }

    public int getOrdem() { return ordem; }
    public int getProfundidade() { return profundidade; }
    public String getCamada() { return camada; }
    public String getComponente() { return componente; }
    public String getOperacao() { return operacao; }
    public double getDuracaoMs() { return duracaoMs; }
}
