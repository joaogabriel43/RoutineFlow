package com.routineflow.unit.usecase;

import com.routineflow.application.usecase.GetHeatmapUseCase;
import com.routineflow.infrastructure.persistence.entity.RoutineJpaEntity;
import com.routineflow.infrastructure.persistence.entity.UserJpaEntity;
import com.routineflow.infrastructure.persistence.repository.DailyLogJpaRepository;
import com.routineflow.infrastructure.persistence.repository.RoutineJpaRepository;
import com.routineflow.infrastructure.persistence.repository.TaskJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetHeatmapUseCaseTest {

    @Mock private RoutineJpaRepository routineJpaRepository;
    @Mock private TaskJpaRepository taskJpaRepository;
    @Mock private DailyLogJpaRepository dailyLogJpaRepository;

    private GetHeatmapUseCase useCase;

    private static final Long USER_ID = 1L;
    // April 14 (Tue) → April 20 (Mon) — 7 days
    private static final LocalDate FROM = LocalDate.of(2026, 4, 14);
    private static final LocalDate TO   = LocalDate.of(2026, 4, 20);

    @BeforeEach
    void setUp() {
        useCase = new GetHeatmapUseCase(routineJpaRepository, taskJpaRepository, dailyLogJpaRepository);
    }

    @Test
    @DisplayName("getHeatmap_someCompletedDays_returnsAllDaysWithCorrectCounts")
    void getHeatmap_someCompletedDays_returnsAllDaysWithCorrectCounts() {
        var user = UserJpaEntity.builder().id(USER_ID).build();
        var routine = RoutineJpaEntity.builder().id(1L).user(user).build();

        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(routine));
        // TUESDAY: 2 tasks, MONDAY: 3 tasks
        when(taskJpaRepository.countByRoutineGroupedByDayOfWeek(1L)).thenReturn(List.of(
                new Object[]{DayOfWeek.TUESDAY, 2L},
                new Object[]{DayOfWeek.MONDAY,  3L}
        ));
        // Apr 14 (Tue) → 1 completion; Apr 20 (Mon) → 2 completions
        when(dailyLogJpaRepository.countCompletedByUserIdGroupedByDate(USER_ID, FROM, TO)).thenReturn(List.of(
                new Object[]{LocalDate.of(2026, 4, 14), 1L},
                new Object[]{LocalDate.of(2026, 4, 20), 2L}
        ));

        var result = useCase.getHeatmap(USER_ID, FROM, TO);

        assertThat(result.from()).isEqualTo(FROM);
        assertThat(result.to()).isEqualTo(TO);
        assertThat(result.days()).hasSize(7);

        var april14 = result.days().stream()
                .filter(d -> d.date().equals(LocalDate.of(2026, 4, 14))).findFirst().orElseThrow();
        assertThat(april14.completedTasks()).isEqualTo(1);
        assertThat(april14.totalTasks()).isEqualTo(2);
        assertThat(april14.completionRate()).isEqualTo(0.5);

        // Apr 15 (Wed) — no tasks scheduled, no completions
        var april15 = result.days().stream()
                .filter(d -> d.date().equals(LocalDate.of(2026, 4, 15))).findFirst().orElseThrow();
        assertThat(april15.completedTasks()).isEqualTo(0);
        assertThat(april15.totalTasks()).isEqualTo(0);
        assertThat(april15.completionRate()).isEqualTo(0.0);

        // Peak day = Apr 20 with 2 completions (highest)
        assertThat(result.peakDay()).isNotNull();
        assertThat(result.peakDay().date()).isEqualTo(LocalDate.of(2026, 4, 20));
        assertThat(result.peakDay().completedTasks()).isEqualTo(2);
        assertThat(result.peakDay().totalTasks()).isEqualTo(3);
    }

    @Test
    @DisplayName("getHeatmap_noCompletionsInRange_allDaysZeroAndNoPeakDay")
    void getHeatmap_noCompletionsInRange_allDaysZeroAndNoPeakDay() {
        var user = UserJpaEntity.builder().id(USER_ID).build();
        var routine = RoutineJpaEntity.builder().id(1L).user(user).build();

        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(routine));
        when(taskJpaRepository.countByRoutineGroupedByDayOfWeek(1L)).thenReturn(List.of());
        when(dailyLogJpaRepository.countCompletedByUserIdGroupedByDate(USER_ID, FROM, TO))
                .thenReturn(List.of());

        var result = useCase.getHeatmap(USER_ID, FROM, TO);

        assertThat(result.days()).hasSize(7);
        result.days().forEach(day -> {
            assertThat(day.completedTasks()).isEqualTo(0);
            assertThat(day.totalTasks()).isEqualTo(0);
            assertThat(day.completionRate()).isEqualTo(0.0);
        });
        assertThat(result.peakDay()).isNull();
    }

    @Test
    @DisplayName("getHeatmap_fromAfterTo_throwsIllegalArgumentException")
    void getHeatmap_fromAfterTo_throwsIllegalArgumentException() {
        LocalDate invalidFrom = LocalDate.of(2026, 4, 20);
        LocalDate invalidTo   = LocalDate.of(2026, 4, 14);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> useCase.getHeatmap(USER_ID, invalidFrom, invalidTo))
                .withMessageContaining("from");
    }

    @Test
    @DisplayName("getHeatmap_rangeOver365Days_throwsIllegalArgumentException")
    void getHeatmap_rangeOver365Days_throwsIllegalArgumentException() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to   = LocalDate.of(2026, 1, 2); // 366 days

        assertThatIllegalArgumentException()
                .isThrownBy(() -> useCase.getHeatmap(USER_ID, from, to))
                .withMessageContaining("365");
    }
}
