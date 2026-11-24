package br.edu.ifpa.saudecerta.cadastro.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tudo o que os outros modulos podem pedir ao cadastro.
 * Se um dia o cadastro virar um servico separado, esta interface vira o
 * contrato remoto - e quem a usa nao precisa mudar.
 */
public interface CadastroFachada {

    Optional<PacienteResumo> paciente(Long id);

    Map<Long, PacienteResumo> pacientes(Collection<Long> ids);

    Optional<ProfissionalResumo> profissional(Long id);

    Map<Long, ProfissionalResumo> profissionais(Collection<Long> ids);

    List<ProfissionalResumo> profissionaisAtivos();

    long totalDePacientesAtivos();
}
