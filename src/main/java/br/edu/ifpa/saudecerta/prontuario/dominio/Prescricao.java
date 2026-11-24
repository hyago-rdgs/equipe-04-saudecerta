package br.edu.ifpa.saudecerta.prontuario.dominio;

import java.util.ArrayList;
import java.util.List;

import br.edu.ifpa.saudecerta.compartilhado.erro.MissaoPendenteException;

/**
 * MISSAO E3-2 - Prescricao vinculada a um atendimento.
 *
 * Este esqueleto existe para a equipe completar. O comportamento esperado esta
 * descrito, como teste, em PrescricaoTest (hoje desativado com @Disabled).
 * Caminho sugerido: reative o teste, veja-o falhar, implemente ate passar.
 *
 * Regras a implementar:
 *  1. uma prescricao tem pelo menos um item;
 *  2. cada item tem medicamento, dose e posologia, todos obrigatorios;
 *  3. o mesmo medicamento nao aparece duas vezes na mesma prescricao;
 *  4. se o paciente tem alerta de alergia que cita o medicamento, a prescricao
 *     e recusada (RegraDeNegocioException) - os alertas vem do cadastro.
 */
public class Prescricao {

    /** Um medicamento prescrito. */
    public record Item(String medicamento, String dose, String posologia) {
    }

    private final Long atendimentoId;
    private final List<Item> itens = new ArrayList<>();

    public Prescricao(Long atendimentoId) {
        this.atendimentoId = atendimentoId;
    }

    public void adicionar(Item item, String alertasDoPaciente) {
        throw new MissaoPendenteException("E3-2", "Prescrição ainda não implementada. Veja a missão E3-2 em MISSOES.md.");
    }

    public void validarParaEmissao() {
        throw new MissaoPendenteException("E3-2", "Prescrição ainda não implementada. Veja a missão E3-2 em MISSOES.md.");
    }

    public Long getAtendimentoId() { return atendimentoId; }
    public List<Item> getItens() { return List.copyOf(itens); }
}
