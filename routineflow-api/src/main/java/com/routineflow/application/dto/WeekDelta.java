package com.routineflow.application.dto;

public record WeekDelta(
        Long areaId,
        String areaName,
        double currentRate,
        Double previousRate,
        Double delta
) {}
