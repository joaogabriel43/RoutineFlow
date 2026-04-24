package com.routineflow.application.dto;

import com.routineflow.domain.model.ResetFrequency;

import java.util.List;

public record AreaResponse(
        Long id,
        String name,
        String color,
        String icon,
        int orderIndex,
        ResetFrequency resetFrequency,
        List<TaskResponse> tasks
) {}
