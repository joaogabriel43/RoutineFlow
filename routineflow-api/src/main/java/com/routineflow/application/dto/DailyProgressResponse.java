package com.routineflow.application.dto;

import java.time.LocalDate;
import java.util.List;

public record DailyProgressResponse(
        LocalDate logDate,
        List<AreaProgressResponse> areas,
        double overallCompletionRate
) {}
