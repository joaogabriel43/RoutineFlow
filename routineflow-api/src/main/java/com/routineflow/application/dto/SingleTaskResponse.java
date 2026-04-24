package com.routineflow.application.dto;

import java.time.Instant;
import java.time.LocalDate;

public record SingleTaskResponse(
        Long id,
        String title,
        String description,
        LocalDate dueDate,
        boolean completed,
        Instant completedAt,
        Instant createdAt,
        boolean isOverdue   // dueDate != null && dueDate.isBefore(today) && !completed
) {}
