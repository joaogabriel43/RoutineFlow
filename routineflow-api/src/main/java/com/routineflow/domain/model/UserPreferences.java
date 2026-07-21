package com.routineflow.domain.model;

import java.time.DayOfWeek;

public record UserPreferences(
    String theme,
    boolean soundEnabled,
    DayOfWeek firstDayOfWeek
) {}
