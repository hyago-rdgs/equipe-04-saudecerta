package br.edu.ifpa.saudecerta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Teste de INTEGRACAO: sobe a aplicacao inteira (com os dados de demonstracao)
 * e exercita a API de ponta a ponta, atravessando todas as camadas.
 * E mais lento que os testes de dominio - compare os tempos no relatorio.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FluxoDaClinicaTest {

    private static final String SENHA = "saude123";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate banco;

    private String entrar(String login) throws Exception {
        String corpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("login", login, "senha", SENHA))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(corpo).get("token").asText();
    }

    /** Um dia util daqui a alguns dias, longe dos dados de demonstracao. */
    private static LocalDate diaUtilFuturo() {
        LocalDate dia = LocalDate.now().plusDays(5);
        return dia.getDayOfWeek() == DayOfWeek.SUNDAY ? dia.plusDays(1) : dia;
    }

    @Test
    @DisplayName("sem token, a API responde 401")
    void exigeAutenticacao() throws Exception {
        mvc.perform(get("/api/pacientes")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("senha errada responde 401")
    void recusaSenhaErrada() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("login", "admin", "senha", "errada"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("administrador nao le prontuario (403), profissional le (200)")
    void prontuarioRestritoAProfissionais() throws Exception {
        mvc.perform(get("/api/pacientes/1/prontuario").header("Authorization", entrar("admin")))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/pacientes/1/prontuario").header("Authorization", entrar("ana")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paciente.nome").exists());
    }

    @Test
    @DisplayName("ler um prontuario deixa rastro na auditoria")
    void leituraDeProntuarioEhAuditada() throws Exception {
        mvc.perform(get("/api/pacientes/2/prontuario").header("Authorization", entrar("rafael")))
                .andExpect(status().isOk());

        String corpo = mvc.perform(get("/api/auditoria").param("pacienteId", "2")
                .header("Authorization", entrar("admin")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        boolean registrado = false;
        for (JsonNode registro : json.readTree(corpo)) {
            if ("PRONTUARIO_LIDO".equals(registro.get("acao").asText())
                    && "rafael".equals(registro.get("usuario").asText())) {
                registrado = true;
            }
        }
        assertThat(registrado).as("leitura do prontuario registrada na auditoria").isTrue();
    }

    @Test
    @DisplayName("recepcao agenda uma consulta (201) e o mesmo horario nao pode ser usado de novo (409)")
    void agendaEDetectaConflito() throws Exception {
        String recepcao = entrar("recepcao");
        String pedido = json.writeValueAsString(Map.of(
                "pacienteId", 3,
                "profissionalId", 4,
                "inicio", diaUtilFuturo() + "T10:00:00"));

        mvc.perform(post("/api/consultas").header("Authorization", recepcao)
                .contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGENDADA"));

        String outroPaciente = json.writeValueAsString(Map.of(
                "pacienteId", 6,
                "profissionalId", 4,
                "inicio", diaUtilFuturo() + "T10:00:00"));

        mvc.perform(post("/api/consultas").header("Authorization", recepcao)
                .contentType(MediaType.APPLICATION_JSON).content(outroPaciente))
                .andExpect(status().isConflict());
    }

    @Test
    @Transactional
    void relatorioContaConsultasNoPeriodoInclusivo() throws Exception {
        LocalDate de = LocalDate.of(2020, 3, 1);
        LocalDate ate = de.plusDays(1);
        inserirConsulta(1L, de.atStartOfDay().minusNanos(1_000), "AGENDADA");
        inserirConsulta(1L, de.atStartOfDay(), "AGENDADA");
        inserirConsulta(1L, de.atTime(10, 0), "AGENDADA");
        inserirConsulta(1L, ate.atTime(15, 0), "REALIZADA");
        inserirConsulta(2L, ate.atTime(23, 59, 59, 999_999_000), "CANCELADA");
        inserirConsulta(2L, ate.plusDays(1).atStartOfDay(), "CANCELADA");

        mvc.perform(relatorio(de, ate, "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.de").value(de.toString()))
                .andExpect(jsonPath("$.ate").value(ate.toString()))
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.porSituacao.AGENDADA").value(2))
                .andExpect(jsonPath("$.porSituacao.REALIZADA").value(1))
                .andExpect(jsonPath("$.porSituacao.CANCELADA").value(1))
                .andExpect(jsonPath("$.porProfissional[0].profissionalId").value(1))
                .andExpect(jsonPath("$.porProfissional[0].nome").value("Dra. Ana Beatriz Moura"))
                .andExpect(jsonPath("$.porProfissional[0].total").value(3))
                .andExpect(jsonPath("$.porProfissional[1].profissionalId").value(2))
                .andExpect(jsonPath("$.porProfissional[1].total").value(1))
                .andExpect(jsonPath("$.porEspecialidade['Clínica Geral']").value(3))
                .andExpect(jsonPath("$.porEspecialidade.Cardiologia").value(1));
    }

    @Test
    void relatorioVazioRetornaZero() throws Exception {
        LocalDate de = LocalDate.of(2020, 1, 1);
        mvc.perform(relatorio(de, de.plusYears(1), "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.porSituacao").isEmpty())
                .andExpect(jsonPath("$.porProfissional").isEmpty())
                .andExpect(jsonPath("$.porEspecialidade").isEmpty());
    }

    @Test
    void relatorioRecusaPeriodosInvalidos() throws Exception {
        LocalDate de = LocalDate.of(2020, 1, 1);
        mvc.perform(relatorio(de, de.minusDays(1), "admin"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("A data final não pode ser anterior à data inicial."));
        mvc.perform(relatorio(de, de.plusYears(1).plusDays(1), "admin"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("O período do relatório não pode ultrapassar um ano."));
    }

    @Test
    void relatorioExigePerfilAdministrativo() throws Exception {
        LocalDate data = LocalDate.of(2020, 1, 1);
        mvc.perform(relatorio(data, data, "recepcao")).andExpect(status().isForbidden());
    }

    private MockHttpServletRequestBuilder relatorio(LocalDate de, LocalDate ate, String login) throws Exception {
        return get("/api/relatorios/consultas").param("de", de.toString()).param("ate", ate.toString())
                .header("Authorization", entrar(login));
    }

    private void inserirConsulta(long profissionalId, LocalDateTime inicio, String situacao) {
        banco.update("""
                insert into consulta (paciente_id, profissional_id, inicio, duracao_minutos, status)
                values (1, ?, ?, 30, ?)
                """, profissionalId, inicio, situacao);
    }

    @Test
    @Transactional
    void remarcacaoExibeVagasSemConflitosERegistraOsDoisHorarios() throws Exception {
        LocalDate dia = diaUtilFuturo();
        LocalDateTime original = dia.atTime(8, 0);
        long id = inserirConsulta(1, 1, original, "CONFIRMADA");
        inserirConsulta(2, 1, dia.atTime(9, 0), "AGENDADA");
        inserirConsulta(1, 2, dia.atTime(10, 0), "AGENDADA");
        String recepcao = entrar("recepcao");

        String resposta = mvc.perform(get("/api/agenda/horarios-livres").header("Authorization", recepcao)
                .param("data", dia.toString()).param("profissionalId", "1").param("consultaId", Long.toString(id)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readValue(resposta, String[].class))
                .contains("11:00:00").doesNotContain("08:00:00", "09:00:00", "10:00:00");

        mvc.perform(remarcacao(id, dia.atTime(9, 0), recepcao)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("O profissional já tem consulta nesse horário."));
        mvc.perform(remarcacao(id, dia.atTime(10, 0), recepcao)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("O paciente já tem outra consulta nesse horário."));

        LocalDateTime novo = dia.atTime(11, 0);
        mvc.perform(remarcacao(id, novo, recepcao)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("AGENDADA"))
                .andExpect(jsonPath("$.inicio").value(startsWith(novo.toString())));

        String registros = mvc.perform(get("/api/auditoria").param("pacienteId", "1")
                .header("Authorization", entrar("admin"))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(registros).contains("CONSULTA_REMARCADA", original.toString(), novo.toString());
    }

    @Test
    @Transactional
    void prescricoesValidamResponsavelEAlergiaEConstamNoHistoricoEAuditoria() throws Exception {
        long consultaId = inserirConsulta(1, 1, LocalDate.of(2020, 5, 1).atTime(10, 0), "CONFIRMADA");
        String ana = entrar("ana");
        String rafael = entrar("rafael");
        String atendimento = mvc.perform(post("/api/atendimentos").header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("consultaId", consultaId))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long id = json.readTree(atendimento).path("id").asLong();
        String url = "/api/atendimentos/" + id + "/prescricoes";

        mvc.perform(prescricao(url, "Medicamento de teste", rafael)).andExpect(status().isForbidden());
        mvc.perform(prescricao(url, "Dipirona 1 g", ana)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(containsString("alergia")));
        mvc.perform(prescricao(url, "Medicamento de teste", ana)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.itens[0].medicamento").value("Medicamento de teste"));
        mvc.perform(get(url).header("Authorization", ana)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itens[0].posologia").value("Posologia de teste"))
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(put("/api/atendimentos/" + id).header("Authorization", ana)
                .contentType(MediaType.APPLICATION_JSON).content("{\"evolucao\":\"Evolução de teste\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/atendimentos/" + id + "/finalizacao").header("Authorization", ana))
                .andExpect(status().isOk());
        mvc.perform(prescricao(url, "Outro medicamento de teste", ana)).andExpect(status().isUnprocessableEntity());

        mvc.perform(get("/api/pacientes/1/prontuario").header("Authorization", rafael))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atendimentos[?(@.id == " + id + ")].prescricoes[0].itens[0].medicamento")
                        .value(hasItem("Medicamento de teste")));
        String registros = mvc.perform(get("/api/auditoria").param("pacienteId", "1")
                .header("Authorization", entrar("admin"))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(registros).contains("PRESCRICAO_EMITIDA", "PRESCRICOES_LIDAS");
    }

    @Test
    @Transactional
    void portalUsaPacienteDoTokenESoCancelaConsultaFuturaPropria() throws Exception {
        LocalDate dia = diaUtilFuturo().plusDays(14);
        long propria = inserirConsulta(1, 1, dia.atTime(8, 0), "AGENDADA");
        long alheia = inserirConsulta(2, 2, dia.atTime(8, 30), "AGENDADA");
        long passada = inserirConsulta(1, 1, LocalDate.of(2020, 5, 1).atTime(9, 0), "AGENDADA");
        String portal = "/api/portal/consultas";
        String joana = entrar("joana");

        mvc.perform(get(portal).header("Authorization", entrar("admin"))).andExpect(status().isForbidden());
        String listagem = mvc.perform(get(portal).param("pacienteId", "2").header("Authorization", joana))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Map<Long, JsonNode> porId = new HashMap<>();
        for (JsonNode registro : json.readTree(listagem)) {
            porId.put(registro.path("id").asLong(), registro);
            assertThat(registro.propertyStream().map(Map.Entry::getKey))
                    .containsExactlyInAnyOrder("id", "inicio", "duracaoMinutos", "status", "profissional",
                            "especialidade", "futura", "cancelavel");
        }
        assertThat(porId).containsKeys(propria, passada).doesNotContainKey(alheia);
        assertThat(porId.get(propria).path("futura").asBoolean()).isTrue();
        assertThat(porId.get(passada).path("futura").asBoolean()).isFalse();

        mvc.perform(get(portal + "/" + alheia).header("Authorization", joana)).andExpect(status().isNotFound());
        mvc.perform(cancelamento(portal, alheia, joana, "Imprevisto")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Consulta não encontrada."));
        mvc.perform(cancelamento(portal, passada, joana, "Imprevisto")).andExpect(status().isUnprocessableEntity());
        mvc.perform(cancelamento(portal, propria, joana, " ")).andExpect(status().isBadRequest());
        mvc.perform(cancelamento(portal, propria, joana, "Imprevisto")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"))
                .andExpect(jsonPath("$.cancelavel").value(false));

        assertThat(banco.queryForObject("select status from consulta where id = ?", String.class, alheia))
                .isEqualTo("AGENDADA");
    }

    @Test
    void reservasConcorrentesDoMesmoProfissionalResultamEmUmaConsulta() throws Exception {
        var inicio = diaUtilFuturo().atTime(14, 0);
        var token = entrar("recepcao");

        verificarUmaReserva(inicio, 2, agendamento(1, 2, inicio, token),
                agendamento(2, 2, inicio, token), "profissional_id");
    }

    @Test
    void reservasConcorrentesDoMesmoPacienteResultamEmUmaConsulta() throws Exception {
        var inicio = diaUtilFuturo().atTime(14, 30);
        var token = entrar("recepcao");

        verificarUmaReserva(inicio, 1, agendamento(1, 1, inicio, token),
                agendamento(1, 2, inicio, token), "paciente_id");
    }

    @Test
    void remarcacaoEAgendamentoDisputamOMesmoHorario() throws Exception {
        var dia = diaUtilFuturo();
        var token = entrar("recepcao");
        var corpo = mvc.perform(agendamento(1, 1, dia.atTime(16, 0), token)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(corpo).path("id").asLong();
        var destino = dia.atTime(16, 30);

        int statusRemarcacao = verificarUmaReserva(destino, 1, remarcacao(id, destino, token),
                agendamento(2, 1, destino, token), "profissional_id");

        assertThat(banco.queryForObject("select inicio from consulta where id = ?", LocalDateTime.class, id))
                .isEqualTo(statusRemarcacao == 200 ? destino : dia.atTime(16, 0));
    }

    @Test
    void reservaConcluiMesmoComMaisRequisicoesDoQueConexoesNoPool() throws Exception {
        var inicio = diaUtilFuturo().atTime(17, 0);
        var token = entrar("recepcao");
        int quantidade = 12;
        var largada = new CyclicBarrier(quantidade);
        var executor = Executors.newFixedThreadPool(quantidade);
        try {
            var pendentes = new ArrayList<Future<MockHttpServletResponse>>();
            for (int i = 0; i < quantidade; i++) {
                var pedido = agendamento(1, 1, inicio, token);
                pendentes.add(executor.submit(() -> requisicaoSimultanea(largada, pedido)));
            }
            var codigos = new ArrayList<Integer>();
            for (var pendente : pendentes) {
                codigos.add(pendente.get(15, TimeUnit.SECONDS).getStatus());
            }
            assertThat(codigos).containsOnly(201, 409).filteredOn(codigo -> codigo == 201).hasSize(1);
            assertThat(banco.queryForObject(
                    "select count(*) from consulta where profissional_id = 1 and inicio = ?",
                    Long.class, inicio)).isEqualTo(1L);
        } finally {
            executor.shutdownNow();
        }
    }

    // Cada chamada HTTP abre e encerra sua própria transação, em uma thread distinta.
    private int verificarUmaReserva(LocalDateTime inicio, int recursoId, MockHttpServletRequestBuilder primeira,
            MockHttpServletRequestBuilder segunda, String coluna) throws Exception {
        var largada = new CyclicBarrier(2);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var uma = executor.submit(() -> requisicaoSimultanea(largada, primeira));
            var outra = executor.submit(() -> requisicaoSimultanea(largada, segunda));
            var respostas = List.of(uma.get(15, TimeUnit.SECONDS), outra.get(15, TimeUnit.SECONDS));
            assertThat(respostas).filteredOn(r -> r.getStatus() == 200 || r.getStatus() == 201).hasSize(1);
            assertThat(respostas).filteredOn(r -> r.getStatus() == 409).singleElement()
                    .satisfies(r -> assertThat(r.getContentAsString()).contains("já tem", "nesse horário"));

            assertThat(banco.queryForObject(
                    "select count(*) from consulta where " + coluna + " = ? and inicio = ? and status <> 'CANCELADA'",
                    Long.class, recursoId, inicio)).isEqualTo(1L);
            return respostas.get(0).getStatus();
        } finally {
            executor.shutdownNow();
        }
    }

    private MockHttpServletResponse requisicaoSimultanea(CyclicBarrier largada, MockHttpServletRequestBuilder pedido)
            throws Exception {
        largada.await(5, TimeUnit.SECONDS);
        return mvc.perform(pedido).andReturn().getResponse();
    }

    private MockHttpServletRequestBuilder agendamento(long pacienteId, long profissionalId, LocalDateTime inicio,
            String token) throws Exception {
        return post("/api/consultas").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("pacienteId", pacienteId, "profissionalId", profissionalId,
                        "inicio", inicio)));
    }

    private long inserirConsulta(long paciente, long profissional, LocalDateTime inicio, String situacao) {
        banco.update("""
                insert into consulta (paciente_id, profissional_id, inicio, duracao_minutos, status)
                values (?, ?, ?, 30, ?)
                """, paciente, profissional, inicio, situacao);
        return banco.queryForObject("select id from consulta where paciente_id = ? and profissional_id = ? and inicio = ?",
                Long.class, paciente, profissional, inicio);
    }

    private MockHttpServletRequestBuilder remarcacao(long id, LocalDateTime inicio, String token) throws Exception {
        return put("/api/consultas/" + id + "/horario").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("inicio", inicio)));
    }

    private MockHttpServletRequestBuilder prescricao(String url, String medicamento, String token) throws Exception {
        return post(url).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("itens", List.of(Map.of(
                        "medicamento", medicamento, "dose", "Dose de teste", "posologia", "Posologia de teste")))));
    }

    private MockHttpServletRequestBuilder cancelamento(String url, long id, String token, String motivo) throws Exception {
        return post(url + "/" + id + "/cancelamento").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("motivo", motivo)));
    }
}
