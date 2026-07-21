package com.routineflow.application.dto;

import java.time.DayOfWeek;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdatePreferencesRequest(
    @NotBlank String theme,
    @NotNull Boolean soundEnabled,
    @NotNull DayOfWeek firstDayOfWeek
) {}
