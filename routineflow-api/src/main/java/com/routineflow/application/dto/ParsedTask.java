package com.routineflow.application.dto;

public record ParsedTask(
        String title,
        String description,
        Integer estimatedMinutes,
        Integer orderIndex
) {}
