package br.edu.ifpa.saudecerta.prontuario.adaptador.persistencia;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import br.edu.ifpa.saudecerta.prontuario.aplicacao.AtendimentoRepositorio;
import br.edu.ifpa.saudecerta.prontuario.dominio.Atendimento;

/** ADAPTADOR DE SAIDA: implementa a porta AtendimentoRepositorio usando Spring Data JPA. */
@Component
public class AtendimentoRepositorioJpa implements AtendimentoRepositorio {

    private final AtendimentoJpaRepository jpa;

    public AtendimentoRepositorioJpa(AtendimentoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Atendimento salvar(Atendimento atendimento) {
        return jpa.save(AtendimentoJpa.de(atendimento)).paraDominio();
    }

    @Override
    public Optional<Atendimento> buscarPorId(Long id) {
        return jpa.findById(id).map(AtendimentoJpa::paraDominio);
    }

    @Override
    public Optional<Atendimento> buscarPorConsulta(Long consultaId) {
        return jpa.findByConsultaId(consultaId).map(AtendimentoJpa::paraDominio);
    }

    @Override
    public List<Atendimento> doPaciente(Long pacienteId) {
        return jpa.findByPacienteIdOrderByInicioDesc(pacienteId).stream().map(AtendimentoJpa::paraDominio).toList();
    }
}
