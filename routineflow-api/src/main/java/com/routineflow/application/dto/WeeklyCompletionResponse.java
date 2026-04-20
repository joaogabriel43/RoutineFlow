package com.routineflow.application.dto;

import java.time.LocalDate;
import java.util.List;

public record WeeklyCompletionResponse(
        LocalDate weekStart,
        LocalDate weekEnd,
        List<WeeklyAreaCompletion> areas,
        double overallRate
) {}
