package com.routineflow.domain.model;

import java.time.DayOfWeek;

public record Task(
        Long id,
        String title,
        String description,
        Integer estimatedMinutes,
        DayOfWeek dayOfWeek,
        Long areaId,
        Integer orderIndex
) {}
