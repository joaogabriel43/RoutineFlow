package com.routineflow.application.dto;

public record WeeklyAreaCompletion(
        Long areaId,
        String areaName,
        String color,
        String icon,
        int completedTasks,
        int totalTasks,
        double completionRate
) {}
