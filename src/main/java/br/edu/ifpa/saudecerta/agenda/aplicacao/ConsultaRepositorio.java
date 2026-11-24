package br.edu.ifpa.saudecerta.agenda.aplicacao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import br.edu.ifpa.saudecerta.agenda.dominio.Consulta;

/**
 * PORTA DE SAIDA. A aplicacao diz DO QUE precisa; quem decide COMO (JPA,
 * arquivo, outro servico...) e um adaptador. Repare: nada aqui menciona banco.
 */
public interface ConsultaRepositorio {

    Consulta salvar(Consulta consulta);

    Optional<Consulta> buscarPorId(Long id);

    List<Consulta> doDia(LocalDate data);

    List<Consulta> doProfissionalNoDia(Long profissionalId, LocalDate data);

    List<Consulta> doPacienteNoDia(Long pacienteId, LocalDate data);
}
