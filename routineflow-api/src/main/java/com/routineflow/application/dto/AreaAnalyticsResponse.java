package com.routineflow.application.dto;

import com.routineflow.domain.model.ResetFrequency;

import java.time.DayOfWeek;
import java.util.List;

public record AreaAnalyticsResponse(
        Long areaId,
        String areaName,
        String color,
        String icon,
        ResetFrequency resetFrequency,

        // Historical totals
        int totalCheckIns,
        int totalExpected,
        double overallCompletionRate,

        // Streak
        int currentStreak,
        int bestStreak,

        // Weekly trend — last 12 weeks
        List<WeeklyTrendPoint> weeklyTrend,

        // Day of week breakdown
        List<DayOfWeekStat> dayOfWeekStats,
        DayOfWeek bestDayOfWeek,
        String bestDayLabel
) {}
