package com.routineflow.integration.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.routineflow.application.dto.LoginRequest;
import com.routineflow.application.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class ExportControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = registerAndGetToken("export_user@test.com", "SecurePass123!");
        importRoutine(token);
    }

    // ── GET /export/checkins ─────────────────────────────────────────────────

    @Test
    @DisplayName("exportCheckIns_authenticatedUser_returns200WithCsvContentType")
    void exportCheckIns_authenticatedUser_returns200WithCsvContentType() throws Exception {
        mockMvc.perform(get("/export/checkins")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }

    @Test
    @DisplayName("exportCheckIns_returns200WithCorrectCsvHeader")
    void exportCheckIns_returns200WithCorrectCsvHeader() throws Exception {
        String body = mockMvc.perform(get("/export/checkins")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        // Strip BOM if present (first char is \uFEFF)
        if (body.startsWith("\uFEFF")) {
            body = body.substring(1);
        }

        String firstLine = body.lines().findFirst().orElse("");
        org.assertj.core.api.Assertions.assertThat(firstLine)
                .isEqualTo("Data,Dia da Semana,Área,Tarefa,Concluído,Horário de Conclusão");
    }

    @Test
    @DisplayName("exportCheckIns_returns200WithContentDispositionFilename")
    void exportCheckIns_returns200WithContentDispositionFilename() throws Exception {
        mockMvc.perform(get("/export/checkins")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        containsString("attachment")))
                .andExpect(header().string("Content-Disposition",
                        containsString("routineflow-export-")));
    }

    @Test
    @DisplayName("exportCheckIns_withDateRange_returns200")
    void exportCheckIns_withDateRange_returns200() throws Exception {
        mockMvc.perform(get("/export/checkins")
                        .param("from", "2026-01-01")
                        .param("to", "2026-04-30")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }

    @Test
    @DisplayName("exportCheckIns_withoutToken_returns401")
    void exportCheckIns_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/export/checkins"))
                .andExpect(status().isUnauthorized());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String registerAndGetToken(String email, String password) throws Exception {
        var register = new RegisterRequest("Export User", email, password);
        var result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andReturn();

        if (result.getResponse().getStatus() == 409) {
            var login = new LoginRequest(email, password);
            result = mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(login)))
                    .andReturn();
        }

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    private void importRoutine(String tok) throws Exception {
        var yaml = getClass().getClassLoader().getResourceAsStream("fixtures/valid_routine.yaml");
        if (yaml == null) return;
        mockMvc.perform(multipart("/routines/import")
                .file(new MockMultipartFile("file", "routine.yaml", "application/x-yaml", yaml))
                .header("Authorization", "Bearer " + tok));
    }
}
