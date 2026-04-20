package com.routineflow.unit.security;

import com.routineflow.infrastructure.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    // Segredo de 256 bits mínimo para HS256
    private static final String TEST_SECRET =
            "test-secret-key-that-is-long-enough-for-hs256-algorithm-min-256-bits";
    private static final long EXPIRATION_MS = 3_600_000L; // 1h

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, EXPIRATION_MS);
    }

    @Test
    @DisplayName("generateToken_validUser_returnsNonBlankToken")
    void generateToken_validUser_returnsNonBlankToken() {
        UserDetails user = buildUser("user@test.com");

        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("extractUsername_validToken_returnsCorrectEmail")
    void extractUsername_validToken_returnsCorrectEmail() {
        UserDetails user = buildUser("user@test.com");
        String token = jwtService.generateToken(user);

        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("isTokenValid_validTokenAndCorrectUser_returnsTrue")
    void isTokenValid_validTokenAndCorrectUser_returnsTrue() {
        UserDetails user = buildUser("user@test.com");
        String token = jwtService.generateToken(user);

        boolean valid = jwtService.isTokenValid(token, user);

        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("isTokenValid_validTokenButDifferentUser_returnsFalse")
    void isTokenValid_validTokenButDifferentUser_returnsFalse() {
        UserDetails owner = buildUser("owner@test.com");
        UserDetails other = buildUser("other@test.com");
        String token = jwtService.generateToken(owner);

        boolean valid = jwtService.isTokenValid(token, other);

        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("isTokenValid_expiredToken_returnsFalse")
    void isTokenValid_expiredToken_returnsFalse() {
        // Token com expiração de -1ms = já expirado na geração
        JwtService shortLivedService = new JwtService(TEST_SECRET, -1L);
        UserDetails user = buildUser("user@test.com");
        String token = shortLivedService.generateToken(user);

        boolean valid = jwtService.isTokenValid(token, user);

        assertThat(valid).isFalse();
    }

    private UserDetails buildUser(String email) {
        return new User(email, "hashed-password", Collections.emptyList());
    }
}
