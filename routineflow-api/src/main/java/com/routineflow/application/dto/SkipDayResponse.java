package com.routineflow.application.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record SkipDayResponse(
        Long id,
        Long areaId,
        LocalDate skipDate,
        String reason,
        OffsetDateTime createdAt
) {}
