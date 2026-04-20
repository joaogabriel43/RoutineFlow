package com.routineflow.integration.area;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.routineflow.application.dto.CreateAreaRequest;
import com.routineflow.application.dto.LoginRequest;
import com.routineflow.application.dto.RegisterRequest;
import com.routineflow.application.dto.ReorderAreasRequest;
import com.routineflow.application.dto.UpdateAreaRequest;
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

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AreaControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        var register = new RegisterRequest("Area User", "areauser@test.com", "SecurePass123!");
        var registerResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andReturn();

        if (registerResult.getResponse().getStatus() == 409) {
            var login = new LoginRequest("areauser@test.com", "SecurePass123!");
            registerResult = mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(login)))
                    .andReturn();
        }

        var body = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        jwtToken = body.get("token").asText();

        // Import a routine so CRUD operations have a context
        importYamlRoutine();
    }

    // ── GET /areas ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAreas_withActiveRoutine_returns200WithAreaList")
    void getAreas_withActiveRoutine_returns200WithAreaList() throws Exception {
        mockMvc.perform(get("/areas")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].name").isString())
                .andExpect(jsonPath("$[0].color").isString())
                .andExpect(jsonPath("$[0].icon").isString())
                .andExpect(jsonPath("$[0].orderIndex").isNumber());
    }

    @Test
    @DisplayName("getAreas_withoutToken_returns401")
    void getAreas_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/areas"))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /areas ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("createArea_validPayload_returns201WithAreaResponse")
    void createArea_validPayload_returns201WithAreaResponse() throws Exception {
        var request = new CreateAreaRequest("Leitura", "#10B981", "📖");

        mockMvc.perform(post("/areas")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Leitura"))
                .andExpect(jsonPath("$.color").value("#10B981"))
                .andExpect(jsonPath("$.icon").value("📖"))
                .andExpect(jsonPath("$.orderIndex").isNumber());
    }

    @Test
    @DisplayName("createArea_invalidPayload_returns400")
    void createArea_invalidPayload_returns400() throws Exception {
        var invalidRequest = new CreateAreaRequest("", "not-a-color", "");

        mockMvc.perform(post("/areas")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    @DisplayName("createArea_noActiveRoutine_returns404")
    void createArea_noActiveRoutine_returns404() throws Exception {
        // Register a fresh user with no imported routine
        var newUser = new RegisterRequest("No Routine Area User", "noroutinearea@test.com", "SecurePass123!");
        var result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andReturn();
        if (result.getResponse().getStatus() == 409) {
            var login = new LoginRequest("noroutinearea@test.com", "SecurePass123!");
            result = mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(login)))
                    .andReturn();
        }
        var noRoutineToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();

        var request = new CreateAreaRequest("Teste", "#3B82F6", "📚");
        mockMvc.perform(post("/areas")
                        .header("Authorization", "Bearer " + noRoutineToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ── PUT /areas/{id} ──────────────────────────────────────────────────────

    @Test
    @DisplayName("updateArea_validPayload_returns200WithUpdatedArea")
    void updateArea_validPayload_returns200WithUpdatedArea() throws Exception {
        // First, get an existing area ID
        var listResult = mockMvc.perform(get("/areas")
                        .header("Authorization", "Bearer " + jwtToken))
                .andReturn();
        var areas = objectMapper.readTree(listResult.getResponse().getContentAsString());
        long areaId = areas.get(0).get("id").asLong();

        var updateRequest = new UpdateAreaRequest("Inglês Avançado", "#60A5FA", "🎯");

        mockMvc.perform(put("/areas/{id}", areaId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(areaId))
                .andExpect(jsonPath("$.name").value("Inglês Avançado"))
                .andExpect(jsonPath("$.color").value("#60A5FA"))
                .andExpect(jsonPath("$.icon").value("🎯"));
    }

    // ── DELETE /areas/{id} ───────────────────────────────────────────────────

    @Test
    @DisplayName("deleteArea_validId_returns204")
    void deleteArea_validId_returns204() throws Exception {
        // Create a throwaway area to delete
        var createRequest = new CreateAreaRequest("Deletar", "#EF4444", "🗑");
        var createResult = mockMvc.perform(post("/areas")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn();
        var createdId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(delete("/areas/{id}", createdId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

    // ── PATCH /areas/reorder ─────────────────────────────────────────────────

    @Test
    @DisplayName("reorderAreas_validPayload_returns200WithReorderedList")
    void reorderAreas_validPayload_returns200WithReorderedList() throws Exception {
        var listResult = mockMvc.perform(get("/areas")
                        .header("Authorization", "Bearer " + jwtToken))
                .andReturn();
        var areas = objectMapper.readTree(listResult.getResponse().getContentAsString());

        // Reverse the current order
        List<Long> reversedIds = new java.util.ArrayList<>();
        for (int i = areas.size() - 1; i >= 0; i--) {
            reversedIds.add(areas.get(i).get("id").asLong());
        }

        var reorderRequest = new ReorderAreasRequest(reversedIds);

        mockMvc.perform(patch("/areas/reorder")
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
