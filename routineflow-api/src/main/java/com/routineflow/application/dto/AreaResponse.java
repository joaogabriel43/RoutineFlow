package com.routineflow.application.dto;

import java.util.List;

public record AreaResponse(
        Long id,
        String name,
        String color,
        String icon,
        int orderIndex,
        List<TaskResponse> tasks
) {}
