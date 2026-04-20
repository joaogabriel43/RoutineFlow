package com.routineflow.application.dto;

import java.time.Instant;
import java.time.LocalDate;

public record DailyLogResponse(
        Long taskId,
        boolean completed,
        Instant completedAt,
        LocalDate logDate
) {}
