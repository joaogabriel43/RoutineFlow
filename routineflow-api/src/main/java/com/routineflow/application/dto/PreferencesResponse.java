package com.routineflow.application.dto;

import java.time.DayOfWeek;

public record PreferencesResponse(
    String theme,
    boolean soundEnabled,
    DayOfWeek firstDayOfWeek
) {}
