package com.routineflow.application.dto;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

public record ParsedArea(
        String name,
        String color,
        String icon,
        Map<DayOfWeek, List<ParsedTask>> schedule
) {}
