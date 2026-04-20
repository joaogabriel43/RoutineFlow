package com.routineflow.application.usecase;

import com.routineflow.application.dto.WeeklyCompletionResponse;
import com.routineflow.application.dto.WeeklyHistoryResponse;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Service
public class GetWeeklyHistoryUseCase {

    private static final int MAX_WEEKS = 12;

    private final GetWeeklyCompletionUseCase weeklyCompletionUseCase;

    public GetWeeklyHistoryUseCase(GetWeeklyCompletionUseCase weeklyCompletionUseCase) {
        this.weeklyCompletionUseCase = weeklyCompletionUseCase;
    }

    public WeeklyHistoryResponse getHistory(Long userId, int weeks, LocalDate referenceDate) {
        if (weeks < 1 || weeks > MAX_WEEKS) {
            throw new IllegalArgumentException("weeks must be between 1 and " + MAX_WEEKS);
        }

        LocalDate currentWeekStart = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        var weekCompletions = new ArrayList<WeeklyCompletionResponse>();
        for (int i = weeks - 1; i >= 0; i--) {
            LocalDate weekStart = currentWeekStart.minusWeeks(i);
            weekCompletions.add(weeklyCompletionUseCase.getWeeklyCompletion(userId, weekStart));
        }

        return new WeeklyHistoryResponse(List.copyOf(weekCompletions));
    }
}
