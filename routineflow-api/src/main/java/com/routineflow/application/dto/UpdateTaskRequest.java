package com.routineflow.application.dto;

import com.routineflow.domain.model.ScheduleType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;

public record UpdateTaskRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must be at most 255 characters")
        String title,

        String description,

        @Positive(message = "Estimated minutes must be a positive number")
        Integer estimatedMinutes,

        @NotNull(message = "scheduleType is required")
        ScheduleType scheduleType,

        DayOfWeek dayOfWeek,

        @Min(value = 1, message = "dayOfMonth must be between 1 and 31")
        @Max(value = 31, message = "dayOfMonth must be between 1 and 31")
        Integer dayOfMonth
) {}
