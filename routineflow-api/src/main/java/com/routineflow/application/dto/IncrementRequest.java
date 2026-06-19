package com.routineflow.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record IncrementRequest(
        @NotNull(message = "Increment is required")
        @Positive(message = "Increment must be greater than zero")
        BigDecimal increment
) {}
