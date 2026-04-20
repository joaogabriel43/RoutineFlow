package com.routineflow.unit.usecase;

import com.routineflow.application.usecase.GetWeeklyCompletionUseCase;
import com.routineflow.infrastructure.persistence.entity.AreaJpaEntity;
import com.routineflow.infrastructure.persistence.entity.RoutineJpaEntity;
import com.routineflow.infrastructure.persistence.entity.TaskJpaEntity;
import com.routineflow.infrastructure.persistence.entity.UserJpaEntity;
import com.routineflow.infrastructure.persistence.repository.AreaJpaRepository;
import com.routineflow.infrastructure.persistence.repository.DailyLogJpaRepository;
import com.routineflow.infrastructure.persistence.repository.RoutineJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetWeeklyCompletionUseCaseTest {

    @Mock private RoutineJpaRepository routineJpaRepository;
    @Mock private AreaJpaRepository areaJpaRepository;
    @Mock private DailyLogJpaRepository dailyLogJpaRepository;

    private GetWeeklyCompletionUseCase useCase;

    private static final Long USER_ID = 1L;
    // Week of Apr 13 (Mon) → Apr 19 (Sun)
    private static final LocalDate WEEK_START = LocalDate.of(2026, 4, 13);
    private static final LocalDate WEEK_END   = LocalDate.of(2026, 4, 19);

    @BeforeEach
    void setUp() {
        useCase = new GetWeeklyCompletionUseCase(routineJpaRepository, areaJpaRepository, dailyLogJpaRepository);
    }

    @Test
    @DisplayName("getWeeklyCompletion_twoAreasPartialCompletion_returnsCorrectRates")
    void getWeeklyCompletion_twoAreasPartialCompletion_returnsCorrectRates() {
        var user = UserJpaEntity.builder().id(USER_ID).build();
        var routine = RoutineJpaEntity.builder().id(1L).user(user).build();

        // Area1: 3 tasks (Mon, Wed, Fri)
        var area1 = buildAreaWithTasks(1L, "Inglês", "#3B82F6", "📚", user, routine,
                DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
        // Area2: 2 tasks (Tue, Thu)
        var area2 = buildAreaWithTasks(2L, "Exercício", "#10B981", "🏋️", user, routine,
                DayOfWeek.TUESDAY, DayOfWeek.THURSDAY);

        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(routine));
        when(areaJpaRepository.findAreasWithAllTasksByRoutineId(1L)).thenReturn(List.of(area1, area2));
        // Area1: 2 completed of 3; Area2: 1 completed of 2
        when(dailyLogJpaRepository.countCompletedByUserIdAndAreaGroupedByArea(USER_ID, WEEK_START, WEEK_END))
                .thenReturn(List.of(
                        new Object[]{1L, 2L},
                        new Object[]{2L, 1L}
                ));

        var result = useCase.getWeeklyCompletion(USER_ID, WEEK_START);

        assertThat(result.weekStart()).isEqualTo(WEEK_START);
        assertThat(result.weekEnd()).isEqualTo(WEEK_END);
        assertThat(result.areas()).hasSize(2);

        var area1Result = result.areas().stream().filter(a -> a.areaId().equals(1L)).findFirst().orElseThrow();
        assertThat(area1Result.totalTasks()).isEqualTo(3);
        assertThat(area1Result.completedTasks()).isEqualTo(2);
        assertThat(area1Result.completionRate()).isCloseTo(0.667, within(0.001));

        var area2Result = result.areas().stream().filter(a -> a.areaId().equals(2L)).findFirst().orElseThrow();
        assertThat(area2Result.totalTasks()).isEqualTo(2);
        assertThat(area2Result.completedTasks()).isEqualTo(1);
        assertThat(area2Result.completionRate()).isEqualTo(0.5);

        // Overall: 3 completed / 5 total = 0.6
        assertThat(result.overallRate()).isCloseTo(0.6, within(0.001));
    }

    @Test
    @DisplayName("getWeeklyCompletion_noCompletionsYet_returnsZeroRates")
    void getWeeklyCompletion_noCompletionsYet_returnsZeroRates() {
        var user = UserJpaEntity.builder().id(USER_ID).build();
        var routine = RoutineJpaEntity.builder().id(1L).user(user).build();
        var area = buildAreaWithTasks(1L, "Leitura", "#F59E0B", "📖", user, routine, DayOfWeek.MONDAY);

        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(routine));
        when(areaJpaRepository.findAreasWithAllTasksByRoutineId(1L)).thenReturn(List.of(area));
        when(dailyLogJpaRepository.countCompletedByUserIdAndAreaGroupedByArea(USER_ID, WEEK_START, WEEK_END))
                .thenReturn(List.of());

        var result = useCase.getWeeklyCompletion(USER_ID, WEEK_START);

        assertThat(result.areas()).hasSize(1);
        assertThat(result.areas().get(0).completedTasks()).isEqualTo(0);
        assertThat(result.areas().get(0).completionRate()).isEqualTo(0.0);
        assertThat(result.overallRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getWeeklyCompletion_noActiveRoutine_returnsEmptyAreas")
    void getWeeklyCompletion_noActiveRoutine_returnsEmptyAreas() {
        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.empty());

        var result = useCase.getWeeklyCompletion(USER_ID, WEEK_START);

        assertThat(result.areas()).isEmpty();
        assertThat(result.overallRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getWeeklyCompletion_weekEnd_isDerivedCorrectlyFromWeekStart")
    void getWeeklyCompletion_weekEnd_isDerivedCorrectlyFromWeekStart() {
        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.empty());

        var result = useCase.getWeeklyCompletion(USER_ID, WEEK_START);

        assertThat(result.weekEnd()).isEqualTo(WEEK_START.plusDays(6));
        assertThat(result.weekEnd().getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
    }

    private AreaJpaEntity buildAreaWithTasks(Long id, String name, String color, String icon,
                                              UserJpaEntity user, RoutineJpaEntity routine,
                                              DayOfWeek... days) {
        List<TaskJpaEntity> tasks = new ArrayList<>();
        for (int i = 0; i < days.length; i++) {
            tasks.add(TaskJpaEntity.builder()
                    .id((long) (id * 10 + i))
                    .title("Task " + i)
                    .dayOfWeek(days[i])
                    .orderIndex(i)
                    .build());
        }
        var area = AreaJpaEntity.builder()
                .id(id).name(name).color(color).icon(icon)
                .user(user).routine(routine).tasks(tasks).build();
        tasks.forEach(t -> t.setArea(area));
        return area;
    }
}
