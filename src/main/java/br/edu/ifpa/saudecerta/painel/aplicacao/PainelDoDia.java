package br.edu.ifpa.saudecerta.painel.aplicacao;

import java.time.LocalDate;
import java.util.List;

import br.edu.ifpa.saudecerta.agenda.api.ConsultaInfo;

public record PainelDoDia(LocalDate data, int total, int agendadas, int aguardando, int emAtendimento,
        int realizadas, int canceladas, long pacientesAtivos, int profissionaisAtivos, List<ConsultaInfo> consultas) {
}
