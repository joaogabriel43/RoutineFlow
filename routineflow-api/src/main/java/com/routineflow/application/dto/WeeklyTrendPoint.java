package com.routineflow.application.dto;

import java.time.LocalDate;

public record WeeklyTrendPoint(
        LocalDate weekStart,
        String weekLabel,
        int completedTasks,
        int totalTasks,
        double completionRate
) {}
