package com.routineflow.application.dto;

import java.time.LocalDate;

public record HeatmapDayResponse(
        LocalDate date,
        int completedTasks,
        int totalTasks,
        double completionRate
) {}
