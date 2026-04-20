package com.routineflow.application.dto;

import java.time.DayOfWeek;
import java.util.List;

public record DayScheduleResponse(
        DayOfWeek dayOfWeek,
        List<AreaWithTasksResponse> areas
) {}
