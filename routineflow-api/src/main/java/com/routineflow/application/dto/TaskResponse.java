package com.routineflow.application.dto;

public record TaskResponse(
        Long id,
        String title,
        String description,
        Integer estimatedMinutes,
        Integer orderIndex
) {}
