package com.routineflow.domain.model;

import java.time.Instant;
import java.time.LocalDate;

public record SingleTask(
        Long id,
        Long userId,
        String title,
        String description,
        LocalDate dueDate,
        boolean completed,
        Instant completedAt,
        Instant createdAt,
        Instant archivedAt
) {}
