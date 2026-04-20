package com.routineflow.application.dto;

import java.time.LocalDate;

public record StreakResponse(
        Long areaId,
        String areaName,
        String color,
        String icon,
        int currentStreak,
        LocalDate lastActiveDate
) {}
