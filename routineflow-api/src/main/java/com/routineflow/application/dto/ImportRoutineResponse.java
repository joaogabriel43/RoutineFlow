package com.routineflow.application.dto;

import java.time.Instant;

public record ImportRoutineResponse(
        Long routineId,
        String name,
        int totalAreas,
        int totalTasks,
        Instant importedAt
) {}
