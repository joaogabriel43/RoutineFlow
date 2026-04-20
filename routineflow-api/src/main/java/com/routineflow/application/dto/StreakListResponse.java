package com.routineflow.application.dto;

import java.util.List;

public record StreakListResponse(
        List<StreakResponse> streaks
) {}
