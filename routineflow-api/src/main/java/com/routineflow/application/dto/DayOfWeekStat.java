package com.routineflow.application.dto;

import java.time.DayOfWeek;

public record DayOfWeekStat(
        DayOfWeek dayOfWeek,
        String dayLabel,
        int completedCount,
        double completionRate
) {}
