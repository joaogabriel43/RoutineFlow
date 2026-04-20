package com.routineflow.application.dto;

public record AreaProgressResponse(
        Long areaId,
        String areaName,
        String color,
        String icon,
        int totalTasks,
        int completedTasks,
        double completionRate
) {}
