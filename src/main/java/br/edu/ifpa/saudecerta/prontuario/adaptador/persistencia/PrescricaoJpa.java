package br.edu.ifpa.saudecerta.prontuario.adaptador.persistencia;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import br.edu.ifpa.saudecerta.prontuario.aplicacao.PrescricaoInfo;
import br.edu.ifpa.saudecerta.prontuario.dominio.Prescricao;
import br.edu.ifpa.saudecerta.prontuario.dominio.Prescricao.Item;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "prescricao")
public class PrescricaoJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long atendimentoId;

    @Column(nullable = false)
    private LocalDateTime emitidaEm;

    @ElementCollection
    @CollectionTable(name = "prescricao_item", joinColumns = @JoinColumn(name = "prescricao_id"))
    @OrderColumn(name = "ordem")
    private List<ItemJpa> itens = new ArrayList<>();

    protected PrescricaoJpa() {
    }

    PrescricaoJpa(Prescricao prescricao, LocalDateTime emitidaEm) {
        this.atendimentoId = prescricao.getAtendimentoId();
        this.emitidaEm = emitidaEm;
        prescricao.getItens().forEach(item -> itens.add(new ItemJpa(item)));
    }

    Long atendimentoId() {
        return atendimentoId;
    }

    PrescricaoInfo paraLeitura() {
        return new PrescricaoInfo(id, emitidaEm, itens.stream().map(ItemJpa::paraLeitura).toList());
    }

    @Embeddable
    static class ItemJpa {
        @Column(nullable = false, length = 200)
        private String medicamento;
        @Column(nullable = false, length = 200)
        private String dose;
        @Column(nullable = false, length = 500)
        private String posologia;

        protected ItemJpa() {
        }

        ItemJpa(Item item) {
            this.medicamento = item.medicamento();
            this.dose = item.dose();
            this.posologia = item.posologia();
        }

        Item paraLeitura() {
            return new Item(medicamento, dose, posologia);
        }
    }
}
