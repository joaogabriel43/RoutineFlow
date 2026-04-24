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
class AreaAnalyticsControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String tokenUserA;
    private long   areaIdUserA;

    @BeforeEach
    void setUp() throws Exception {
        // User A: has an active routine with areas
        tokenUserA = registerAndGetToken("areaanalytics_a@test.com", "SecurePass123!");
        importRoutine(tokenUserA);

        // Grab the first area that belongs to user A
        var areasResult = mockMvc.perform(get("/areas")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andReturn();
        var areas = objectMapper.readTree(areasResult.getResponse().getContentAsString());
        areaIdUserA = areas.get(0).get("id").asLong();
    }

    // ── GET /areas/{id}/analytics ─────────────────────────────────────────────

    @Test
    @DisplayName("getAreaAnalytics_validAreaAndOwner_returns200WithFullResponse")
    void getAreaAnalytics_validAreaAndOwner_returns200WithFullResponse() throws Exception {
        mockMvc.perform(get("/areas/{id}/analytics", areaIdUserA)
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.areaId").value(areaIdUserA))
                .andExpect(jsonPath("$.areaName").isString())
                .andExpect(jsonPath("$.color").isString())
                .andExpect(jsonPath("$.icon").isString())
                .andExpect(jsonPath("$.resetFrequency").isString())
                .andExpect(jsonPath("$.totalCheckIns").isNumber())
                .andExpect(jsonPath("$.totalExpected").isNumber())
                .andExpect(jsonPath("$.overallCompletionRate").isNumber())
                .andExpect(jsonPath("$.currentStreak").isNumber())
                .andExpect(jsonPath("$.bestStreak").isNumber())
                .andExpect(jsonPath("$.weeklyTrend").isArray())
                .andExpect(jsonPath("$.weeklyTrend.length()").value(12))
                .andExpect(jsonPath("$.dayOfWeekStats").isArray());
    }

    @Test
    @DisplayName("getAreaAnalytics_anotherUsersArea_returns404")
    void getAreaAnalytics_anotherUsersArea_returns404() throws Exception {
        // User B has no routine — their call for user A's area returns 404 (ADR-006)
        String tokenUserB = registerAndGetToken("areaanalytics_b@test.com", "SecurePass123!");

        mockMvc.perform(get("/areas/{id}/analytics", areaIdUserA)
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("getAreaAnalytics_nonExistentArea_returns404")
    void getAreaAnalytics_nonExistentArea_returns404() throws Exception {
        mockMvc.perform(get("/areas/{id}/analytics", 999_999L)
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("getAreaAnalytics_withoutToken_returns401")
    void getAreaAnalytics_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/areas/{id}/analytics", areaIdUserA))
                .andExpect(status().isUnauthorized());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

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

    private void importRoutine(String token) throws Exception {
        var yaml = getClass().getClassLoader().getResourceAsStream("fixtures/valid_routine.yaml");
        if (yaml == null) return;
        mockMvc.perform(multipart("/routines/import")
                .file(new MockMultipartFile("file", "routine.yaml", "application/x-yaml", yaml))
                .header("Authorization", "Bearer " + token));
    }
}
