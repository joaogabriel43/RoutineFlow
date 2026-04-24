package com.routineflow.application.dto;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Projection record used for CSV export. Constructed directly by JPQL
 * using the constructor expression — never serialised to JSON.
 */
public record CheckInExportRow(
        LocalDate logDate,
        DayOfWeek dayOfWeek,
        String areaName,
        String taskTitle,
        boolean completed,
        Instant completedAt   // nullable
) {}
