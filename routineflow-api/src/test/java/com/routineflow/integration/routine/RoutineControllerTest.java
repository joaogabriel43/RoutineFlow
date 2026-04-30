package com.routineflow.integration.routine;

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

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class RoutineControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        // Registra e loga um usuário para obter JWT
        var register = new RegisterRequest("Routine User", "routineuser@test.com", "SecurePass123!");
        var registerResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andReturn();

        // Se já existe (segunda execução), faz login
        if (registerResult.getResponse().getStatus() == 409) {
            var login = new LoginRequest("routineuser@test.com", "SecurePass123!");
            registerResult = mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(login)))
                    .andReturn();
        }

        var body = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        jwtToken = body.get("token").asText();
    }

    // ── Import ───────────────────────────────────────────────

    @Test
    @DisplayName("importRoutine_validYaml_returns201WithRoutineData")
    void importRoutine_validYaml_returns201WithRoutineData() throws Exception {
        var yaml = getClass().getClassLoader().getResourceAsStream("fixtures/valid_routine.yaml");

        mockMvc.perform(multipart("/routines/import")
                        .file(new MockMultipartFile("file", "routine.yaml",
                                "application/x-yaml", yaml))
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.routineId").isNumber())
                .andExpect(jsonPath("$.name").value("Minha Rotina 2026"))
                .andExpect(jsonPath("$.totalAreas").value(2))
                .andExpect(jsonPath("$.totalTasks").value(greaterThan(0)));
    }

    @Test
    @DisplayName("importRoutine_validTxt_returns201")
    void importRoutine_validTxt_returns201() throws Exception {
        var txt = getClass().getClassLoader().getResourceAsStream("fixtures/valid_routine.txt");

        mockMvc.perform(multipart("/routines/import")
                        .file(new MockMultipartFile("file", "routine.txt", "text/plain", txt))
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Minha Rotina TXT"));
    }

    @Test
    @DisplayName("importRoutine_invalidYaml_returns422")
    void importRoutine_invalidYaml_returns422() throws Exception {
        String badYaml = "routine:\n  areas:\n    - name: Área";
        var file = new MockMultipartFile("file", "bad.yaml",
                "application/x-yaml", badYaml.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/routines/import")
                        .file(file)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    @DisplayName("importRoutine_withoutToken_returns401")
    void importRoutine_withoutToken_returns401() throws Exception {
        var file = new MockMultipartFile("file", "routine.yaml",
                "application/x-yaml", "content".getBytes());
        mockMvc.perform(multipart("/routines/import").file(file))
                .andExpect(status().isUnauthorized());
    }

    // ── Queries ───────────────────────────────────────────────

    @Test
    @DisplayName("getActiveRoutine_afterImport_returns200")
    void getActiveRoutine_afterImport_returns200() throws Exception {
        importYamlRoutine();

        mockMvc.perform(get("/routines/active")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Minha Rotina 2026"));
    }

    @Test
    @DisplayName("getActiveRoutine_withoutImport_returns404")
    void getActiveRoutine_withoutImport_returns404() throws Exception {
        // Usuário sem rotina — cria conta nova
        var register = new RegisterRequest("No Routine", "noroutine@test.com", "SecurePass123!");
        var result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andReturn();
        var noRoutineToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();

        mockMvc.perform(get("/routines/active")
                        .header("Authorization", "Bearer " + noRoutineToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("getTodaySchedule_afterImport_returns200WithAreas")
    void getTodaySchedule_afterImport_returns200WithAreas() throws Exception {
        importYamlRoutine();

        mockMvc.perform(get("/routines/active/today")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayOfWeek").isString())
                .andExpect(jsonPath("$.areas").isArray());
    }

    @Test
    @DisplayName("getDaySchedule_mondayAfterImport_returns200WithCorrectTasks")
    void getDaySchedule_mondayAfterImport_returns200WithCorrectTasks() throws Exception {
        importYamlRoutine();

        mockMvc.perform(get("/routines/active/day/MONDAY")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.areas[0].tasks", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("getDaySchedule_withoutToken_returns401")
    void getDaySchedule_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/routines/active/day/MONDAY"))
                .andExpect(status().isUnauthorized());
    }

    // ── Import mode ───────────────────────────────────────────

    @Test
    @DisplayName("importRoutine_withModeReplace_returns201AndDeactivatesPrevious")
    void importRoutine_withModeReplace_returns201AndDeactivatesPrevious() throws Exception {
        importYamlRoutine(); // first import

        var yaml = getClass().getClassLoader().getResourceAsStream("fixtures/valid_routine.yaml");

        mockMvc.perform(multipart("/routines/import")
                        .file(new MockMultipartFile("file", "routine.yaml",
                                "application/x-yaml", yaml))
                        .param("mode", "REPLACE")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mode").value("REPLACE"))
                .andExpect(jsonPath("$.areasCreated").value(2))
                .andExpect(jsonPath("$.areasMerged").value(0))
                .andExpect(jsonPath("$.tasksSkipped").value(0));
    }

    @Test
    @DisplayName("importRoutine_withModeMerge_returns201AndKeepsPreviousActive")
    void importRoutine_withModeMerge_returns201AndKeepsPreviousActive() throws Exception {
        importYamlRoutine(); // creates "Minha Rotina 2026" as active

        var mergeYaml = getClass().getClassLoader().getResourceAsStream("fixtures/merge_routine.yaml");

        mockMvc.perform(multipart("/routines/import")
                        .file(new MockMultipartFile("file", "merge.yaml",
                                "application/x-yaml", mergeYaml))
                        .param("mode", "MERGE")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mode").value("MERGE"))
                .andExpect(jsonPath("$.areasCreated").value(1))   // "Física" is new
                .andExpect(jsonPath("$.areasMerged").value(1))    // "Inglês/PTE" existed
                .andExpect(jsonPath("$.tasksCreated").value(2))   // 1 new task each
                .andExpect(jsonPath("$.tasksSkipped").value(0));
    }

    @Test
    @DisplayName("importRoutine_withoutModeParam_defaultsToReplace")
    void importRoutine_withoutModeParam_defaultsToReplace() throws Exception {
        var yaml = getClass().getClassLoader().getResourceAsStream("fixtures/valid_routine.yaml");

        mockMvc.perform(multipart("/routines/import")
                        .file(new MockMultipartFile("file", "routine.yaml",
                                "application/x-yaml", yaml))
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mode").value("REPLACE"));
    }

    @Test
    @DisplayName("importRoutine_withInvalidMode_returns400")
    void importRoutine_withInvalidMode_returns400() throws Exception {
        var yaml = getClass().getClassLoader().getResourceAsStream("fixtures/valid_routine.yaml");

        mockMvc.perform(multipart("/routines/import")
                        .file(new MockMultipartFile("file", "routine.yaml",
                                "application/x-yaml", yaml))
                        .param("mode", "INVALID_MODE")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isBadRequest());
    }

    private void importYamlRoutine() throws Exception {
        var yaml = getClass().getClassLoader().getResourceAsStream("fixtures/valid_routine.yaml");
        mockMvc.perform(multipart("/routines/import")
                .file(new MockMultipartFile("file", "routine.yaml", "application/x-yaml", yaml))
                .header("Authorization", "Bearer " + jwtToken));
    }
}
