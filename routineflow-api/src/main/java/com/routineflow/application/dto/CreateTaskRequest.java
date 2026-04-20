package com.routineflow.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;

public record CreateTaskRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must be at most 255 characters")
        String title,

        String description,

        @Positive(message = "Estimated minutes must be a positive number")
        Integer estimatedMinutes,

        @NotNull(message = "Day of week is required")
        DayOfWeek dayOfWeek
) {}
