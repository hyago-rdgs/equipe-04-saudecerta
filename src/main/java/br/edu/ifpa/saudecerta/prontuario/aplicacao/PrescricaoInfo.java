package br.edu.ifpa.saudecerta.prontuario.aplicacao;

import java.time.LocalDateTime;
import java.util.List;

import br.edu.ifpa.saudecerta.prontuario.dominio.Prescricao.Item;

public record PrescricaoInfo(Long id, LocalDateTime emitidaEm, List<Item> itens) {
}
