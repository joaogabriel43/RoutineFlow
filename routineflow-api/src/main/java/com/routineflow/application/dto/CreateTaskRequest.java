package com.routineflow.application.dto;

import com.routineflow.domain.model.ScheduleType;
import com.routineflow.domain.model.GoalType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.math.BigDecimal;

public record CreateTaskRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must be at most 255 characters")
        String title,

        String description,

        @Positive(message = "Estimated minutes must be a positive number")
        Integer estimatedMinutes,

        /** Required — must be DAY_OF_WEEK or DAY_OF_MONTH. */
        @NotNull(message = "scheduleType is required")
        ScheduleType scheduleType,

        /** Required when scheduleType = DAY_OF_WEEK; null for DAY_OF_MONTH. */
        DayOfWeek dayOfWeek,

        /** Required when scheduleType = DAY_OF_MONTH (1-31); null for DAY_OF_WEEK. */
        @Min(value = 1, message = "dayOfMonth must be between 1 and 31")
        @Max(value = 31, message = "dayOfMonth must be between 1 and 31")
        Integer dayOfMonth,

        /** Optional — time of day for push notification reminder (HH:mm). */
        LocalTime reminderTime,

        /** Optional — defaults to BOOLEAN if null. */
        GoalType goalType,

        /** Required when goalType = NUMERIC. */
        @Positive(message = "Goal target must be positive")
        BigDecimal goalTarget,

        /** Optional label for NUMERIC goals (e.g. pages, cups). */
        @Size(max = 30, message = "Goal unit must be at most 30 characters")
        String goalUnit
) {}
