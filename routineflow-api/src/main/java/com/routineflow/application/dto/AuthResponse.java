package com.routineflow.application.dto;

import java.time.Instant;

public record AuthResponse(
        String token,
        String name,
        String email,
        Instant expiresAt
) {}
