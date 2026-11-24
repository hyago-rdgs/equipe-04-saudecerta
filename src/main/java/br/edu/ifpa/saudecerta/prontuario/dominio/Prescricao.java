package br.edu.ifpa.saudecerta.prontuario.dominio;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import br.edu.ifpa.saudecerta.compartilhado.erro.RegraDeNegocioException;

/** Prescrição vinculada a um atendimento. */
public class Prescricao {

    public record Item(String medicamento, String dose, String posologia) {
    }

    private static final Pattern CONCENTRACAO = Pattern.compile(
            "\\s+\\d+(?:[.,]\\d+)?\\s*(?:mcg|µg|μg|mg|kg|g|ml|ui|iu|%)\\b|\\s+\\d+(?:[.,]\\d+)?\\s*%");

    private final Long atendimentoId;
    private final List<Item> itens = new ArrayList<>();

    public Prescricao(Long atendimentoId) {
        this.atendimentoId = atendimentoId;
    }

    public void adicionar(Item item, String alertasDoPaciente) {
        if (item == null || incompleto(item.medicamento(), 200) || incompleto(item.dose(), 200)
                || incompleto(item.posologia(), 500)) {
            throw new RegraDeNegocioException("Informe medicamento e dose (até 200 caracteres cada) e posologia (até 500).");
        }
        String medicamento = nomeDoMedicamento(item.medicamento());
        if (medicamento.isBlank()) {
            throw new RegraDeNegocioException("Informe o nome do medicamento.");
        }
        if (itens.stream().anyMatch(outro -> nomeDoMedicamento(outro.medicamento()).equals(medicamento))) {
            throw new RegraDeNegocioException("O medicamento já está nesta prescrição.");
        }
        String alertas = alertasDoPaciente == null ? "" : normalizar(alertasDoPaciente);
        if ((" " + alertas + " ").contains(" " + medicamento + " ")) {
            throw new RegraDeNegocioException("Medicamento citado nos alertas do paciente. Verifique o alerta de alergia antes de prescrever.");
        }
        itens.add(new Item(item.medicamento().strip(), item.dose().strip(), item.posologia().strip()));
    }

    public void validarParaEmissao() {
        if (itens.isEmpty()) {
            throw new RegraDeNegocioException("Inclua pelo menos um medicamento para emitir a prescrição.");
        }
    }

    private static boolean incompleto(String valor, int limite) {
        return valor == null || valor.isBlank() || valor.strip().length() > limite;
    }

    private static String nomeDoMedicamento(String valor) {
        String minusculo = valor.strip().toLowerCase(Locale.ROOT);
        var concentracao = CONCENTRACAO.matcher(minusculo);
        return normalizar(concentracao.find() ? minusculo.substring(0, concentracao.start()) : minusculo);
    }

    private static String normalizar(String valor) {
        return Normalizer.normalize(valor.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^\\p{L}\\p{N}]+", " ").strip();
    }

    public Long getAtendimentoId() { return atendimentoId; }
    public List<Item> getItens() { return List.copyOf(itens); }
}
