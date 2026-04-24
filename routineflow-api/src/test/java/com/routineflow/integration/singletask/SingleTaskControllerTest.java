package com.routineflow.integration.singletask;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class SingleTaskControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        jwtToken = registerAndGetToken("singletask@test.com", "SecurePass123!");
    }

    @Test
    @DisplayName("createSingleTask_validRequest_returns201")
    void createSingleTask_validRequest_returns201() throws Exception {
        var body = Map.of("title", "Comprar leite");

        mockMvc.perform(post("/single-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Comprar leite"))
                .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    @DisplayName("createSingleTask_emptyTitle_returns400")
    void createSingleTask_emptyTitle_returns400() throws Exception {
        var body = Map.of("title", "");

        mockMvc.perform(post("/single-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("createSingleTask_pastDueDate_returns400")
    void createSingleTask_pastDueDate_returns400() throws Exception {
        String yesterday = LocalDate.now().minusDays(1).toString();
        var body = Map.of("title", "Reunião", "dueDate", yesterday);

        mockMvc.perform(post("/single-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("listPendingSingleTasks_returns200WithPendingTasks")
    void listPendingSingleTasks_returns200WithPendingTasks() throws Exception {
        // Create a task first
        var body = Map.of("title", "Tarefa pendente");
        mockMvc.perform(post("/single-tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
                .header("Authorization", "Bearer " + jwtToken));

        mockMvc.perform(get("/single-tasks")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("listArchivedSingleTasks_returns200WithArchivedTasks")
    void listArchivedSingleTasks_returns200WithArchivedTasks() throws Exception {
        mockMvc.perform(get("/single-tasks/archived")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("completeSingleTask_pendingTask_returns200WithCompletedTrue")
    void completeSingleTask_pendingTask_returns200WithCompletedTrue() throws Exception {
        Long taskId = createTask("Tarefa para completar");

        mockMvc.perform(post("/single-tasks/" + taskId + "/complete")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    @Test
    @DisplayName("completeSingleTask_alreadyComplete_returns409")
    void completeSingleTask_alreadyComplete_returns409() throws Exception {
        Long taskId = createTask("Tarefa já completa");

        // Complete once
        mockMvc.perform(post("/single-tasks/" + taskId + "/complete")
                .header("Authorization", "Bearer " + jwtToken));

        // Try to complete again → 409
        mockMvc.perform(post("/single-tasks/" + taskId + "/complete")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("deleteSingleTask_ownedTask_returns204")
    void deleteSingleTask_ownedTask_returns204() throws Exception {
        Long taskId = createTask("Tarefa para deletar");

        mockMvc.perform(delete("/single-tasks/" + taskId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("listTodaySingleTasks_returns200")
    void listTodaySingleTasks_returns200() throws Exception {
        mockMvc.perform(get("/single-tasks/today")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("createSingleTask_withoutToken_returns401")
    void createSingleTask_withoutToken_returns401() throws Exception {
        var body = Map.of("title", "Sem token");

        mockMvc.perform(post("/single-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("listPending_withoutToken_returns401")
    void listPending_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/single-tasks"))
                .andExpect(status().isUnauthorized());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Long createTask(String title) throws Exception {
        var body = Map.of("title", title);
        var response = mockMvc.perform(post("/single-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .header("Authorization", "Bearer " + jwtToken))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
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
