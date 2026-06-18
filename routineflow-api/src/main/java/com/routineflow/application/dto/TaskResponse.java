package com.routineflow.application.dto;

import com.routineflow.domain.model.ScheduleType;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record TaskResponse(
        Long id,
        String title,
        String description,
        Integer estimatedMinutes,
        Integer orderIndex,
        ScheduleType scheduleType,
        DayOfWeek dayOfWeek,   // null when scheduleType = DAY_OF_MONTH
        Integer dayOfMonth,    // null when scheduleType = DAY_OF_WEEK
        LocalTime reminderTime // null when no reminder configured
) {}
