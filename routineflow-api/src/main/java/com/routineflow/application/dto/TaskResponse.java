package com.routineflow.application.dto;

import java.time.DayOfWeek;

public record TaskResponse(
        Long id,
        String title,
        String description,
        Integer estimatedMinutes,
        Integer orderIndex,
        DayOfWeek dayOfWeek
) {}
