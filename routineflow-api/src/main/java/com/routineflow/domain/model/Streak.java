package com.routineflow.domain.model;

import java.time.LocalDate;

public record Streak(
        Long id,
        Long areaId,
        Long userId,
        Integer currentCount,
        LocalDate lastActiveDate
) {}
