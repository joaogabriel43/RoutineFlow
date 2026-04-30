package com.routineflow.application.dto;

import com.routineflow.domain.model.ScheduleType;

import java.time.DayOfWeek;

public record TaskResponse(
        Long id,
        String title,
        String description,
        Integer estimatedMinutes,
        Integer orderIndex,
        ScheduleType scheduleType,
        DayOfWeek dayOfWeek,   // null when scheduleType = DAY_OF_MONTH
        Integer dayOfMonth     // null when scheduleType = DAY_OF_WEEK
) {}
