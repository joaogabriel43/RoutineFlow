package com.routineflow.application.dto;

import java.time.Instant;

public record RoutineResponse(
        Long id,
        String name,
        Instant importedAt,
        int totalAreas,
        int totalTasks
) {}
