package com.routineflow.application.dto;

import com.routineflow.domain.model.ScheduleType;
import com.routineflow.domain.model.GoalType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.math.BigDecimal;

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
        Integer dayOfMonth,

        /** Optional — time of day for push notification reminder (HH:mm). */
        LocalTime reminderTime,

        GoalType goalType,

        @Positive(message = "Goal target must be positive")
        BigDecimal goalTarget,

        @Size(max = 30, message = "Goal unit must be at most 30 characters")
        String goalUnit,

        /** Optional lucide icon name (kebab-case). */
        @Size(max = 50, message = "Icon must be at most 50 characters")
        String icon,

        /** Optional hex color (#RRGGBB). @Pattern skips null, so it stays optional. */
        @Pattern(regexp = "#[0-9A-Fa-f]{6}", message = "Color must be a valid hex code (e.g. #2F8BFF)")
        String color
) {}
