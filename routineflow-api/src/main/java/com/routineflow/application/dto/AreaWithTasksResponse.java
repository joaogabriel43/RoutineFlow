package com.routineflow.application.dto;

import java.util.List;

public record AreaWithTasksResponse(
        Long id,
        String name,
        String color,
        String icon,
        List<TaskResponse> tasks
) {}
