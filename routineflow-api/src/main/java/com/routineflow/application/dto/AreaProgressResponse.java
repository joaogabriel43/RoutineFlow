package com.routineflow.application.dto;

import java.util.List;

public record AreaProgressResponse(
        Long areaId,
        String areaName,
        String color,
        String icon,
        int totalTasks,
        int completedTasks,
        double completionRate,
        List<Long> completedTaskIds
) {}
