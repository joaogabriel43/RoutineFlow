package com.routineflow.domain.model;

import java.time.Instant;
import java.time.LocalDate;

public record DailyLog(
        Long id,
        Long taskId,
        Long userId,
        LocalDate logDate,
        boolean completed,
        Instant completedAt
) {}
