package com.routineflow.integration.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.routineflow.application.dto.CreateTaskRequest;
import com.routineflow.application.dto.LoginRequest;
import com.routineflow.application.dto.RegisterRequest;
import com.routineflow.application.dto.ReorderTasksRequest;
import com.routineflow.application.dto.UpdateTaskRequest;
import com.routineflow.domain.model.ScheduleType;
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

        importYamlRoutine();

        var areasResult = mockMvc.perform(get("/areas")
                        .header("Authorization", "Bearer " + jwtToken))
                .andReturn();
        var areas = objectMapper.readTree(areasResult.getResponse().getContentAsString());
        areaId = areas.get(0).get("id").asLong();
    }

    // ── POST /areas/{areaId}/tasks — DAY_OF_WEEK ─────────────────────────────

    @Test
    @DisplayName("createTask_dayOfWeek_validPayload_returns201WithTaskResponse")
    void createTask_dayOfWeek_validPayload_returns201WithTaskResponse() throws Exception {
        var request = new CreateTaskRequest(
                "Shadowing", "10min audio shadowing", 10,
                ScheduleType.DAY_OF_WEEK, DayOfWeek.FRIDAY, null);

        mockMvc.perform(post("/areas/{areaId}/tasks", areaId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Shadowing"))
                .andExpect(jsonPath("$.scheduleType").value("DAY_OF_WEEK"))
                .andExpect(jsonPath("$.dayOfWeek").value("FRIDAY"))
                .andExpect(jsonPath("$.dayOfMonth").doesNotExist())
                .andExpect(jsonPath("$.orderIndex").isNumber());
    }

    @Test
    @DisplayName("createTask_dayOfWeek_missingScheduleType_returns400")
    void createTask_dayOfWeek_missingScheduleType_returns400() throws Exception {
        // scheduleType is @NotNull — must fail bean validation
        var invalidRequest = new CreateTaskRequest("", null, -5, null, null, null);

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
        var request = new CreateTaskRequest(
                "Test", null, null,
                ScheduleType.DAY_OF_WEEK, DayOfWeek.MONDAY, null);

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

    // ── POST /areas/{areaId}/tasks — DAY_OF_MONTH ────────────────────────────

    @Test
    @DisplayName("createTask_dayOfMonth_validPayload_returns201")
    void createTask_dayOfMonth_validPayload_returns201() throws Exception {
        var request = new CreateTaskRequest(
                "Monthly Bill", "Pay invoice", null,
                ScheduleType.DAY_OF_MONTH, null, 25);

        mockMvc.perform(post("/areas/{areaId}/tasks", areaId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Monthly Bill"))
                .andExpect(jsonPath("$.scheduleType").value("DAY_OF_MONTH"))
                .andExpect(jsonPath("$.dayOfMonth").value(25))
                .andExpect(jsonPath("$.dayOfWeek").doesNotExist());
    }

    @Test
    @DisplayName("createTask_dayOfMonth_withoutDayOfMonth_returns400")
    void createTask_dayOfMonth_withoutDayOfMonth_returns400() throws Exception {
        var request = new CreateTaskRequest(
                "Missing Day", null, null,
                ScheduleType.DAY_OF_MONTH, null, null); // dayOfMonth missing

        mockMvc.perform(post("/areas/{areaId}/tasks", areaId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /areas/{areaId}/tasks/{id} ────────────────────────────────────────

    @Test
    @DisplayName("updateTask_validPayload_returns200WithUpdatedTask")
    void updateTask_validPayload_returns200WithUpdatedTask() throws Exception {
        var createRequest = new CreateTaskRequest(
                "Original Title", "original desc", 15,
                ScheduleType.DAY_OF_WEEK, DayOfWeek.THURSDAY, null);
        var createResult = mockMvc.perform(post("/areas/{areaId}/tasks", areaId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn();
        var taskId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();

        var updateRequest = new UpdateTaskRequest(
                "Updated Title", "updated desc", 25,
                ScheduleType.DAY_OF_WEEK, DayOfWeek.SATURDAY, null);

        mockMvc.perform(put("/areas/{areaId}/tasks/{id}", areaId, taskId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.scheduleType").value("DAY_OF_WEEK"))
                .andExpect(jsonPath("$.dayOfWeek").value("SATURDAY"));
    }

    // ── DELETE /areas/{areaId}/tasks/{id} ─────────────────────────────────────

    @Test
    @DisplayName("deleteTask_validId_returns204")
    void deleteTask_validId_returns204() throws Exception {
        var createRequest = new CreateTaskRequest(
                "To Delete", null, null,
                ScheduleType.DAY_OF_WEEK, DayOfWeek.SUNDAY, null);
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
        var req1 = new CreateTaskRequest(
                "Task Alpha", null, null,
                ScheduleType.DAY_OF_WEEK, DayOfWeek.MONDAY, null);
        var req2 = new CreateTaskRequest(
                "Task Beta", null, null,
                ScheduleType.DAY_OF_WEEK, DayOfWeek.MONDAY, null);

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
