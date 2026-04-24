package com.routineflow.domain.model;

public record Area(
        Long id,
        String name,
        String color,
        String icon,
        Long userId,
        ResetFrequency resetFrequency
) {}
