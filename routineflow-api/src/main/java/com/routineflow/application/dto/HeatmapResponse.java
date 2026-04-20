package com.routineflow.application.dto;

import java.time.LocalDate;
import java.util.List;

public record HeatmapResponse(
        LocalDate from,
        LocalDate to,
        List<HeatmapDayResponse> days,
        HeatmapDayResponse peakDay
) {}
