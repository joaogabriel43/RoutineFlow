package com.routineflow.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;

public record DailyLogResponse(
        Long taskId,
        boolean completed,
        Instant completedAt,
        LocalDate logDate,
        String notes,
        BigDecimal goalProgress,
        BigDecimal goalTarget,
        String goalUnit
) {}
