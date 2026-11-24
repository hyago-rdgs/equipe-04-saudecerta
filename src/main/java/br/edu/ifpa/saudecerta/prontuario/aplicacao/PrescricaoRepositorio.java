package br.edu.ifpa.saudecerta.prontuario.aplicacao;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import br.edu.ifpa.saudecerta.prontuario.dominio.Prescricao;

public interface PrescricaoRepositorio {

    PrescricaoInfo salvar(Prescricao prescricao, LocalDateTime emitidaEm);

    Map<Long, List<PrescricaoInfo>> dosAtendimentos(Collection<Long> atendimentoIds);
}
