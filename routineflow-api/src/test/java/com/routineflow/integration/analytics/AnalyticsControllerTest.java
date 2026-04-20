package com.routineflow.integration.analytics;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AnalyticsControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        jwtToken = registerAndGetToken("analyticsuser@test.com", "SecurePass123!");

        var yamlStream = getClass().getClassLoader().getResourceAsStream("fixtures/valid_routine.yaml");
        mockMvc.perform(multipart("/routines/import")
                .file(new MockMultipartFile("file", "routine.yaml", "application/x-yaml", yamlStream))
                .header("Authorization", "Bearer " + jwtToken));
    }

    // ── Streaks ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getStreaks_afterRoutineImport_returns200WithStreaksList")
    void getStreaks_afterRoutineImport_returns200WithStreaksList() throws Exception {
        mockMvc.perform(get("/analytics/streaks")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streaks").isArray());
    }

    @Test
    @DisplayName("getStreaks_withoutToken_returns401")
    void getStreaks_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/analytics/streaks"))
                .andExpect(status().isUnauthorized());
    }

    // ── Heatmap ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getHeatmap_defaultParams_returns200WithAllDays")
    void getHeatmap_defaultParams_returns200WithAllDays() throws Exception {
        mockMvc.perform(get("/analytics/heatmap")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").isString())
                .andExpect(jsonPath("$.to").isString())
                .andExpect(jsonPath("$.days").isArray());
    }

    @Test
    @DisplayName("getHeatmap_explicitValidRange_returns200")
    void getHeatmap_explicitValidRange_returns200() throws Exception {
        mockMvc.perform(get("/analytics/heatmap")
                        .param("from", "2026-01-01")
                        .param("to",   "2026-04-19")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days").isArray());
    }

    @Test
    @DisplayName("getHeatmap_fromAfterTo_returns400")
    void getHeatmap_fromAfterTo_returns400() throws Exception {
        mockMvc.perform(get("/analytics/heatmap")
                        .param("from", "2026-04-20")
                        .param("to",   "2026-04-14")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("getHeatmap_rangeOver365Days_returns400")
    void getHeatmap_rangeOver365Days_returns400() throws Exception {
        mockMvc.perform(get("/analytics/heatmap")
                        .param("from", "2024-01-01")
                        .param("to",   "2025-01-02")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("getHeatmap_withoutToken_returns401")
    void getHeatmap_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/analytics/heatmap"))
                .andExpect(status().isUnauthorized());
    }

    // ── Weekly ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getCurrentWeek_afterRoutineImport_returns200WithWeekBoundaries")
    void getCurrentWeek_afterRoutineImport_returns200WithWeekBoundaries() throws Exception {
        mockMvc.perform(get("/analytics/weekly/current")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").isString())
                .andExpect(jsonPath("$.weekEnd").isString())
                .andExpect(jsonPath("$.areas").isArray())
                .andExpect(jsonPath("$.overallRate").isNumber());
    }

    @Test
    @DisplayName("getWeekComparison_afterRoutineImport_returns200WithCurrentAndPrevious")
    void getWeekComparison_afterRoutineImport_returns200WithCurrentAndPrevious() throws Exception {
        mockMvc.perform(get("/analytics/weekly/comparison")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentWeek").exists())
                .andExpect(jsonPath("$.previousWeek").exists())
                .andExpect(jsonPath("$.deltas").isArray());
    }

    @Test
    @DisplayName("getWeeklyHistory_defaultWeeks_returns200WithCorrectCount")
    void getWeeklyHistory_defaultWeeks_returns200WithCorrectCount() throws Exception {
        mockMvc.perform(get("/analytics/weekly/history")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeks").isArray())
                .andExpect(jsonPath("$.weeks.length()").value(8));
    }

    @Test
    @DisplayName("getWeeklyHistory_weeksOver12_returns400")
    void getWeeklyHistory_weeksOver12_returns400() throws Exception {
        mockMvc.perform(get("/analytics/weekly/history")
                        .param("weeks", "13")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("getWeeklyHistory_weeksZero_returns400")
    void getWeeklyHistory_weeksZero_returns400() throws Exception {
        mockMvc.perform(get("/analytics/weekly/history")
                        .param("weeks", "0")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("weeklyEndpoints_withoutToken_returns401")
    void weeklyEndpoints_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/analytics/weekly/current"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/analytics/weekly/comparison"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/analytics/weekly/history"))
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
