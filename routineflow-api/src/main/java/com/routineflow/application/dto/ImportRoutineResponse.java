package com.routineflow.application.dto;

import com.routineflow.domain.model.ImportMode;

import java.time.Instant;

public record ImportRoutineResponse(
        Long routineId,
        String name,
        int totalAreas,
        int totalTasks,
        Instant importedAt,
        ImportMode mode,
        int areasCreated,
        int areasMerged,
        int tasksCreated,
        int tasksSkipped
) {}
