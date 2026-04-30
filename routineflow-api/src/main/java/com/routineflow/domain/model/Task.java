package com.routineflow.domain.model;

import java.time.DayOfWeek;

public record Task(
        Long id,
        String title,
        String description,
        Integer estimatedMinutes,
        DayOfWeek dayOfWeek,     // null when scheduleType = DAY_OF_MONTH
        Long areaId,
        Integer orderIndex,
        ScheduleType scheduleType,
        Integer dayOfMonth       // null when scheduleType = DAY_OF_WEEK (1-31)
) {}
