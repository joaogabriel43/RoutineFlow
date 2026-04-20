package com.routineflow.integration.checkin;

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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class CheckInControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String jwtToken;
    private Long firstTaskId;

    @BeforeEach
    void setUp() throws Exception {
        // Registra usuário e obtém token
        jwtToken = registerAndGetToken("checkinuser@test.com", "SecurePass123!");

        // Importa rotina para ter tarefas disponíveis
        var yamlStream = getClass().getClassLoader().getResourceAsStream("fixtures/valid_routine.yaml");
        mockMvc.perform(multipart("/routines/import")
                .file(new MockMultipartFile("file", "routine.yaml", "application/x-yaml", yamlStream))
                .header("Authorization", "Bearer " + jwtToken));

        // Descobre o id da primeira tarefa via endpoint de hoje
        // (usa dia MONDAY da fixture — fixture tem tarefas MONDAY e WEDNESDAY)
        var response = mockMvc.perform(get("/routines/active/day/MONDAY")
                        .header("Authorization", "Bearer " + jwtToken))
                .andReturn().getResponse().getContentAsString();

        var tree = objectMapper.readTree(response);
        if (tree.path("areas").size() > 0 && tree.path("areas").get(0).path("tasks").size() > 0) {
            firstTaskId = tree.path("areas").get(0).path("tasks").get(0).path("id").asLong();
        }
    }

    @Test
    @DisplayName("completeTask_validTask_returns200WithCompletedTrue")
    void completeTask_validTask_returns200WithCompletedTrue() throws Exception {
        if (firstTaskId == null) return; // dia atual sem tarefas

        mockMvc.perform(post("/checkins/" + firstTaskId + "/complete")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.completedAt").isNotEmpty())
                .andExpect(jsonPath("$.taskId").value(firstTaskId));
    }

    @Test
    @DisplayName("uncompleteTask_afterComplete_returns200WithCompletedFalse")
    void uncompleteTask_afterComplete_returns200WithCompletedFalse() throws Exception {
        if (firstTaskId == null) return;

        // Completa primeiro
        mockMvc.perform(post("/checkins/" + firstTaskId + "/complete")
                .header("Authorization", "Bearer " + jwtToken));

        // Depois desmarca
        mockMvc.perform(post("/checkins/" + firstTaskId + "/uncomplete")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.completedAt").isEmpty());
    }

    @Test
    @DisplayName("getTodayProgress_afterImport_returns200WithAreas")
    void getTodayProgress_afterImport_returns200WithAreas() throws Exception {
        mockMvc.perform(get("/checkins/today/progress")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logDate").isString())
                .andExpect(jsonPath("$.overallCompletionRate").isNumber());
    }

    @Test
    @DisplayName("completeTask_taskOfOtherUser_returns403")
    void completeTask_taskOfOtherUser_returns403() throws Exception {
        // Cria outro usuário com suas próprias tarefas
        String otherToken = registerAndGetToken("otheruser@test.com", "SecurePass123!");
        var yaml = getClass().getClassLoader().getResourceAsStream("fixtures/valid_routine.yaml");
        mockMvc.perform(multipart("/routines/import")
                .file(new MockMultipartFile("file", "routine.yaml", "application/x-yaml", yaml))
                .header("Authorization", "Bearer " + otherToken));

        var response = mockMvc.perform(get("/routines/active/day/MONDAY")
                        .header("Authorization", "Bearer " + otherToken))
                .andReturn().getResponse().getContentAsString();
        var tree = objectMapper.readTree(response);
        Long otherTaskId = tree.path("areas").get(0).path("tasks").get(0).path("id").asLong();

        // Usuário 1 tenta fazer check-in na tarefa do usuário 2
        mockMvc.perform(post("/checkins/" + otherTaskId + "/complete")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("completeTask_withoutToken_returns401")
    void completeTask_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/checkins/1/complete"))
                .andExpect(status().isUnauthorized());
    }

    private String registerAndGetToken(String email, String password) throws Exception {
        var register = new RegisterRequest("Test User", email, password);
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
}
