package br.edu.ifpa.saudecerta.compartilhado.raiox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.stereotype.Component;

/** Mantem em memoria os ultimos rastros. Nao e persistido: e uma ferramenta de aula. */
@Component
public class RaioXArmazem {

    private static final int LIMITE = 60;

    private final ConcurrentLinkedDeque<Rastro> rastros = new ConcurrentLinkedDeque<>();

    void guardar(Rastro rastro) {
        rastros.addFirst(rastro);
        while (rastros.size() > LIMITE) {
            rastros.pollLast();
        }
    }

    public Optional<Rastro> buscar(String id) {
        return rastros.stream().filter(r -> r.getId().equals(id)).findFirst();
    }

    public List<Rastro> recentes() {
        return new ArrayList<>(rastros);
    }
}
