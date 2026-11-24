package br.edu.ifpa.saudecerta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
    @DisplayName("funcionalidades deixadas como missao respondem 501")
    void missoesRespondem501() throws Exception {
        mvc.perform(get("/api/atendimentos/1/prescricoes").header("Authorization", entrar("ana")))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.missao").value("E3-2"));
    }
}
