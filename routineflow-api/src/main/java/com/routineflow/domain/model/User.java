package com.routineflow.domain.model;

import java.time.Instant;

public record User(
        Long id,
        String email,
        String name,
        Instant createdAt
) {}
