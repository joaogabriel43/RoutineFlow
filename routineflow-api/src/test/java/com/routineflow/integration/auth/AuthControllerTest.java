package com.routineflow.integration.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.routineflow.application.dto.LoginRequest;
import com.routineflow.application.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("register_validPayload_returns201WithToken")
    void register_validPayload_returns201WithToken() throws Exception {
        var request = new RegisterRequest("João Gabriel", "joao@test.com", "SecurePass123!");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("joao@test.com"))
                .andExpect(jsonPath("$.name").value("João Gabriel"))
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    @DisplayName("register_duplicateEmail_returns409")
    void register_duplicateEmail_returns409() throws Exception {
        var request = new RegisterRequest("User One", "duplicate@test.com", "SecurePass123!");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("register_invalidEmail_returns400")
    void register_invalidEmail_returns400() throws Exception {
        var request = new RegisterRequest("User", "not-an-email", "SecurePass123!");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("login_validCredentials_returns200WithToken")
    void login_validCredentials_returns200WithToken() throws Exception {
        // Arrange: registra antes de logar
        var register = new RegisterRequest("Login User", "loginuser@test.com", "SecurePass123!");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        // Act: login
        var login = new LoginRequest("loginuser@test.com", "SecurePass123!");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("loginuser@test.com"));
    }

    @Test
    @DisplayName("login_wrongPassword_returns401")
    void login_wrongPassword_returns401() throws Exception {
        var register = new RegisterRequest("Wrong Pass User", "wrongpass@test.com", "CorrectPass123!");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        var login = new LoginRequest("wrongpass@test.com", "WrongPassword!");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("protectedEndpoint_withoutToken_returns401")
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/areas"))
                .andExpect(status().isUnauthorized());
    }
}
