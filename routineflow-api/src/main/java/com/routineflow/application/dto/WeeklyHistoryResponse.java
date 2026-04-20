package com.routineflow.application.dto;

import java.util.List;

public record WeeklyHistoryResponse(
        List<WeeklyCompletionResponse> weeks
) {}
