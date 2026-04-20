package com.routineflow.application.dto;

import java.util.List;

public record WeekComparisonResponse(
        WeeklyCompletionResponse currentWeek,
        WeeklyCompletionResponse previousWeek,
        List<WeekDelta> deltas
) {}
