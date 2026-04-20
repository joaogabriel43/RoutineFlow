package com.routineflow.application.usecase;

import com.routineflow.application.dto.WeekComparisonResponse;
import com.routineflow.application.dto.WeekDelta;
import com.routineflow.application.dto.WeeklyAreaCompletion;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GetWeekComparisonUseCase {

    private final GetWeeklyCompletionUseCase weeklyCompletionUseCase;

    public GetWeekComparisonUseCase(GetWeeklyCompletionUseCase weeklyCompletionUseCase) {
        this.weeklyCompletionUseCase = weeklyCompletionUseCase;
    }

    public WeekComparisonResponse getComparison(Long userId, LocalDate referenceDate) {
        LocalDate currentWeekStart  = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate previousWeekStart = currentWeekStart.minusWeeks(1);

        var currentWeek  = weeklyCompletionUseCase.getWeeklyCompletion(userId, currentWeekStart);
        var previousWeek = weeklyCompletionUseCase.getWeeklyCompletion(userId, previousWeekStart);

        Map<Long, Double> previousRates = previousWeek.areas().stream()
                .collect(Collectors.toMap(WeeklyAreaCompletion::areaId, WeeklyAreaCompletion::completionRate));

        List<WeekDelta> deltas = currentWeek.areas().stream()
                .map(area -> {
                    Double prevRate = previousRates.get(area.areaId());
                    Double delta = prevRate != null ? area.completionRate() - prevRate : null;
                    return new WeekDelta(area.areaId(), area.areaName(), area.completionRate(), prevRate, delta);
                })
                .collect(Collectors.toList());

        return new WeekComparisonResponse(currentWeek, previousWeek, deltas);
    }
}
