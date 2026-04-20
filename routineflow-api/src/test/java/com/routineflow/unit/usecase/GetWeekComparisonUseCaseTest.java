package com.routineflow.unit.usecase;

import com.routineflow.application.dto.WeeklyAreaCompletion;
import com.routineflow.application.dto.WeeklyCompletionResponse;
import com.routineflow.application.usecase.GetWeekComparisonUseCase;
import com.routineflow.application.usecase.GetWeeklyCompletionUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetWeekComparisonUseCaseTest {

    @Mock private GetWeeklyCompletionUseCase weeklyCompletionUseCase;

    private GetWeekComparisonUseCase useCase;

    private static final Long USER_ID = 1L;
    // Reference date: Apr 19 (Sun) — current week = Apr 13–19, previous = Apr 6–12
    private static final LocalDate REFERENCE_DATE     = LocalDate.of(2026, 4, 19);
    private static final LocalDate CURRENT_WEEK_START = LocalDate.of(2026, 4, 13);
    private static final LocalDate PREVIOUS_WEEK_START = LocalDate.of(2026, 4, 6);

    @BeforeEach
    void setUp() {
        useCase = new GetWeekComparisonUseCase(weeklyCompletionUseCase);
    }

    @Test
    @DisplayName("getComparison_bothWeeksHaveData_returnsCorrectDeltas")
    void getComparison_bothWeeksHaveData_returnsCorrectDeltas() {
        var currentArea1  = new WeeklyAreaCompletion(1L, "Inglês", "#3B82F6", "📚", 4, 5, 0.8);
        var currentArea2  = new WeeklyAreaCompletion(2L, "Exercício", "#10B981", "🏋️", 3, 5, 0.6);
        var previousArea1 = new WeeklyAreaCompletion(1L, "Inglês", "#3B82F6", "📚", 3, 5, 0.6);
        var previousArea2 = new WeeklyAreaCompletion(2L, "Exercício", "#10B981", "🏋️", 2, 5, 0.4);

        var currentWeek  = new WeeklyCompletionResponse(CURRENT_WEEK_START,  CURRENT_WEEK_START.plusDays(6),  List.of(currentArea1, currentArea2),  0.7);
        var previousWeek = new WeeklyCompletionResponse(PREVIOUS_WEEK_START, PREVIOUS_WEEK_START.plusDays(6), List.of(previousArea1, previousArea2), 0.5);

        when(weeklyCompletionUseCase.getWeeklyCompletion(USER_ID, CURRENT_WEEK_START)).thenReturn(currentWeek);
        when(weeklyCompletionUseCase.getWeeklyCompletion(USER_ID, PREVIOUS_WEEK_START)).thenReturn(previousWeek);

        var result = useCase.getComparison(USER_ID, REFERENCE_DATE);

        assertThat(result.currentWeek().overallRate()).isEqualTo(0.7);
        assertThat(result.previousWeek().overallRate()).isEqualTo(0.5);
        assertThat(result.deltas()).hasSize(2);

        var delta1 = result.deltas().stream().filter(d -> d.areaId().equals(1L)).findFirst().orElseThrow();
        assertThat(delta1.currentRate()).isEqualTo(0.8);
        assertThat(delta1.previousRate()).isEqualTo(0.6);
        assertThat(delta1.delta()).isCloseTo(0.2, within(0.001));

        var delta2 = result.deltas().stream().filter(d -> d.areaId().equals(2L)).findFirst().orElseThrow();
        assertThat(delta2.currentRate()).isEqualTo(0.6);
        assertThat(delta2.previousRate()).isEqualTo(0.4);
        assertThat(delta2.delta()).isCloseTo(0.2, within(0.001));
    }

    @Test
    @DisplayName("getComparison_noPreviousWeekData_previousRateAndDeltaAreNull")
    void getComparison_noPreviousWeekData_previousRateAndDeltaAreNull() {
        var currentArea = new WeeklyAreaCompletion(1L, "Inglês", "#3B82F6", "📚", 3, 5, 0.6);
        var currentWeek  = new WeeklyCompletionResponse(CURRENT_WEEK_START,  CURRENT_WEEK_START.plusDays(6),  List.of(currentArea),  0.6);
        var previousWeek = new WeeklyCompletionResponse(PREVIOUS_WEEK_START, PREVIOUS_WEEK_START.plusDays(6), List.of(), 0.0);

        when(weeklyCompletionUseCase.getWeeklyCompletion(USER_ID, CURRENT_WEEK_START)).thenReturn(currentWeek);
        when(weeklyCompletionUseCase.getWeeklyCompletion(USER_ID, PREVIOUS_WEEK_START)).thenReturn(previousWeek);

        var result = useCase.getComparison(USER_ID, REFERENCE_DATE);

        assertThat(result.deltas()).hasSize(1);
        var delta = result.deltas().get(0);
        assertThat(delta.currentRate()).isEqualTo(0.6);
        assertThat(delta.previousRate()).isNull();
        assertThat(delta.delta()).isNull();
    }

    @Test
    @DisplayName("getComparison_noActiveRoutine_bothWeeksEmpty")
    void getComparison_noActiveRoutine_bothWeeksEmpty() {
        var emptyWeek = new WeeklyCompletionResponse(CURRENT_WEEK_START, CURRENT_WEEK_START.plusDays(6), List.of(), 0.0);
        var emptyPrev = new WeeklyCompletionResponse(PREVIOUS_WEEK_START, PREVIOUS_WEEK_START.plusDays(6), List.of(), 0.0);

        when(weeklyCompletionUseCase.getWeeklyCompletion(USER_ID, CURRENT_WEEK_START)).thenReturn(emptyWeek);
        when(weeklyCompletionUseCase.getWeeklyCompletion(USER_ID, PREVIOUS_WEEK_START)).thenReturn(emptyPrev);

        var result = useCase.getComparison(USER_ID, REFERENCE_DATE);

        assertThat(result.currentWeek().areas()).isEmpty();
        assertThat(result.previousWeek().areas()).isEmpty();
        assertThat(result.deltas()).isEmpty();
    }
}
