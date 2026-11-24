package br.edu.ifpa.saudecerta.apoio;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import br.edu.ifpa.saudecerta.cadastro.api.CadastroFachada;
import br.edu.ifpa.saudecerta.cadastro.api.PacienteResumo;
import br.edu.ifpa.saudecerta.cadastro.api.ProfissionalResumo;
import br.edu.ifpa.saudecerta.seguranca.api.Perfil;
import br.edu.ifpa.saudecerta.seguranca.api.UsuarioLogado;

public final class CenarioEmMemoria {

    public static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 16, 7, 0);
    public static final Clock RELOGIO = Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    public static final UsuarioLogado RECEPCAO = new UsuarioLogado("recepcao", "Recepção", Perfil.RECEPCAO, null, null);
    public static final UsuarioLogado ANA = new UsuarioLogado("ana", "Dra. Ana", Perfil.PROFISSIONAL, 1L, null);
    public static final UsuarioLogado RAFAEL = new UsuarioLogado("rafael", "Dr. Rafael", Perfil.PROFISSIONAL, 2L, null);
    public static final UsuarioLogado JOANA = new UsuarioLogado("joana", "Joana", Perfil.PACIENTE, null, 1L);

    private CenarioEmMemoria() {
    }

    public static class CadastroEmMemoria implements CadastroFachada {
        private final Map<Long, PacienteResumo> pacientes = Map.of(
                1L, paciente(1L, "Joana", "Alergia a dipirona", true),
                2L, paciente(2L, "Miguel", null, true),
                3L, paciente(3L, "Paciente inativo", null, false));
        private final Map<Long, ProfissionalResumo> profissionais = Map.of(
                1L, new ProfissionalResumo(1L, "Dra. Ana", "CRM-1", "Clínica", true),
                2L, new ProfissionalResumo(2L, "Dr. Rafael", "CRM-2", "Cardiologia", true));

        @Override
        public void bloquearParaReserva(Long profissionalId, Long pacienteId) {
            // O adaptador em memória não abre transações. O bloqueio real é coberto na integração.
        }

        @Override
        public Optional<PacienteResumo> paciente(Long id) {
            return Optional.ofNullable(pacientes.get(id));
        }

        @Override
        public Map<Long, PacienteResumo> pacientes(Collection<Long> ids) {
            return ids.stream().distinct().flatMap(id -> paciente(id).stream())
                    .collect(Collectors.toMap(PacienteResumo::id, Function.identity()));
        }

        @Override
        public Optional<ProfissionalResumo> profissional(Long id) {
            return Optional.ofNullable(profissionais.get(id));
        }

        @Override
        public Map<Long, ProfissionalResumo> profissionais(Collection<Long> ids) {
            return ids.stream().distinct().flatMap(id -> profissional(id).stream())
                    .collect(Collectors.toMap(ProfissionalResumo::id, Function.identity()));
        }

        @Override
        public List<ProfissionalResumo> profissionaisAtivos() {
            return profissionais.values().stream().filter(ProfissionalResumo::ativo).toList();
        }

        @Override
        public long totalDePacientesAtivos() {
            return pacientes.values().stream().filter(PacienteResumo::ativo).count();
        }

        private static PacienteResumo paciente(Long id, String nome, String alertas, boolean ativo) {
            return new PacienteResumo(id, nome, "", LocalDate.of(1990, 1, 1), alertas, ativo);
        }
    }
}
