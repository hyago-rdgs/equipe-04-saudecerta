package br.edu.ifpa.saudecerta.cadastro.web;

import br.edu.ifpa.saudecerta.cadastro.modelo.Profissional;

public record ProfissionalResposta(Long id, String nome, String registro, Long especialidadeId,
        String especialidade, boolean ativo) {

    public static ProfissionalResposta de(Profissional p) {
        return new ProfissionalResposta(p.getId(), p.getNome(), p.getRegistro(), p.getEspecialidade().getId(),
                p.getEspecialidade().getNome(), p.isAtivo());
    }
}
