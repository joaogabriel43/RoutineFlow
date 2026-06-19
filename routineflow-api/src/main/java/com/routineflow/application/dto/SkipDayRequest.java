package com.routineflow.application.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SkipDayRequest(
        @NotNull LocalDate date,
        String reason
) {}
