package br.edu.ifpa.saudecerta.compartilhado.raiox;

/** Guarda o rastro da requisicao em andamento (uma por thread). */
final class RaioXContexto {

    private static final ThreadLocal<Rastro> ATUAL = new ThreadLocal<>();

    private RaioXContexto() {
    }

    static void iniciar(Rastro rastro) {
        ATUAL.set(rastro);
    }

    static Rastro atual() {
        return ATUAL.get();
    }

    static void limpar() {
        ATUAL.remove();
    }
}
