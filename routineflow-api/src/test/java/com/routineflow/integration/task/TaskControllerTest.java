package com.routineflow.integration.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.routineflow.application.dto.CreateTaskRequest;
import com.routineflow.application.dto.LoginRequest;
import com.routineflow.application.dto.RegisterRequest;
import com.routineflow.application.dto.ReorderTasksRequest;
import com.routineflow.application.dto.UpdateTaskRequest;
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

import java.time.DayOfWeek;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class TaskControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String jwtToken;
    private Long areaId;

    @BeforeEach
    void setUp() throws Exception {
        // Register or login
        var register = new RegisterRequest("Task User", "taskuser@test.com", "SecurePass123!");
        var registerResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andReturn();

        if (registerResult.getResponse().getStatus() == 409) {
            var login = new LoginRequest("taskuser@test.com", "SecurePass123!");
            registerResult = mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(login)))
                    .andReturn();
        }

        var body = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        jwtToken = body.get("token").asText();

        // Import routine to get an active routine with areas
        importYamlRoutine();

        // Resolve the first area ID dynamically — area IDs change each test run (new import = new entities)
        var areasResult = mockMvc.perform(get("/areas")
                        .header("Authorization", "Bearer " + jwtToken))
                .andReturn();
        var areas = objectMapper.readTree(areasResult.getResponse().getContentAsString());
        areaId = areas.get(0).get("id").asLong();
    }

    // ── POST /areas/{areaId}/tasks ────────────────────────────────────────────

    @Test
    @DisplayName("createTask_validPayload_returns201WithTaskResponse")
    void createTask_validPayload_returns201WithTaskResponse() throws Exception {
        var request = new CreateTaskRequest("Shadowing", "10min audio shadowing", 10, DayOfWeek.FRIDAY);

        mockMvc.perform(post("/areas/{areaId}/tasks", areaId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Shadowing"))
                .andExpect(jsonPath("$.description").value("10min audio shadowing"))
                .andExpect(jsonPath("$.estimatedMinutes").value(10))
                .andExpect(jsonPath("$.dayOfWeek").value("FRIDAY"))
                .andExpect(jsonPath("$.orderIndex").isNumber());
    }

    @Test
    @DisplayName("createTask_invalidPayload_returns400")
    void createTask_invalidPayload_returns400() throws Exception {
        // Missing title (blank) and missing dayOfWeek
        var invalidRequest = new CreateTaskRequest("", null, -5, null);

        mockMvc.perform(post("/areas/{areaId}/tasks", areaId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    @DisplayName("createTask_areaNotFound_returns404")
    void createTask_areaNotFound_returns404() throws Exception {
        var request = new CreateTaskRequest("Test", null, null, DayOfWeek.MONDAY);

        mockMvc.perform(post("/areas/{areaId}/tasks", 999999L)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("anyEndpoint_withoutToken_returns401")
    void anyEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/areas/{areaId}/tasks", areaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ── PUT /areas/{areaId}/tasks/{id} ────────────────────────────────────────

    @Test
    @DisplayName("updateTask_validPayload_returns200WithUpdatedTask")
    void updateTask_validPayload_returns200WithUpdatedTask() throws Exception {
        // Create a task to update
        var createRequest = new CreateTaskRequest("Original Title", "original desc", 15, DayOfWeek.THURSDAY);
        var createResult = mockMvc.perform(post("/areas/{areaId}/tasks", areaId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn();
        var taskId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();

        var updateRequest = new UpdateTaskRequest("Updated Title", "updated desc", 25, DayOfWeek.SATURDAY);

        mockMvc.perform(put("/areas/{areaId}/tasks/{id}", areaId, taskId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.description").value("updated desc"))
                .andExpect(jsonPath("$.estimatedMinutes").value(25))
                .andExpect(jsonPath("$.dayOfWeek").value("SATURDAY"));
    }

    // ── DELETE /areas/{areaId}/tasks/{id} ─────────────────────────────────────

    @Test
    @DisplayName("deleteTask_validId_returns204")
    void deleteTask_validId_returns204() throws Exception {
        // Create a task then delete it
        var createRequest = new CreateTaskRequest("To Delete", null, null, DayOfWeek.SUNDAY);
        var createResult = mockMvc.perform(post("/areas/{areaId}/tasks", areaId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn();
        var taskId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(delete("/areas/{areaId}/tasks/{id}", areaId, taskId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

    // ── PATCH /areas/{areaId}/tasks/reorder ──────────────────────────────────

    @Test
    @DisplayName("reorderTasks_validPayload_returns200WithReorderedList")
    void reorderTasks_validPayload_returns200WithReorderedList() throws Exception {
        // Create two tasks to reorder
        var req1 = new CreateTaskRequest("Task Alpha", null, null, DayOfWeek.MONDAY);
        var req2 = new CreateTaskRequest("Task Beta", null, null, DayOfWeek.MONDAY);

        var r1 = mockMvc.perform(post("/areas/{areaId}/tasks", areaId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andReturn();
        var r2 = mockMvc.perform(post("/areas/{areaId}/tasks", areaId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andReturn();

        var id1 = objectMapper.readTree(r1.getResponse().getContentAsString()).get("id").asLong();
        var id2 = objectMapper.readTree(r2.getResponse().getContentAsString()).get("id").asLong();

        // Reverse: Beta first, Alpha second
        var reorderRequest = new ReorderTasksRequest(List.of(id2, id1));

        mockMvc.perform(patch("/areas/{areaId}/tasks/reorder", areaId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reorderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void importYamlRoutine() throws Exception {
        var yaml = getClass().getClassLoader().getResourceAsStream("fixtures/valid_routine.yaml");
        if (yaml == null) return;
        mockMvc.perform(multipart("/routines/import")
                .file(new MockMultipartFile("file", "routine.yaml", "application/x-yaml", yaml))
                .header("Authorization", "Bearer " + jwtToken));
    }
}
