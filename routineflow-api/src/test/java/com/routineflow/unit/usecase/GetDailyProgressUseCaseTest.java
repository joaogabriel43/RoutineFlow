package com.routineflow.unit.usecase;

import com.routineflow.application.usecase.GetDailyProgressUseCase;
import com.routineflow.domain.model.ScheduleType;
import com.routineflow.infrastructure.persistence.entity.AreaJpaEntity;
import com.routineflow.infrastructure.persistence.entity.DailyLogJpaEntity;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetDailyProgressUseCaseTest {

    @Mock private RoutineJpaRepository routineJpaRepository;
    @Mock private AreaJpaRepository areaJpaRepository;
    @Mock private DailyLogJpaRepository dailyLogJpaRepository;

    private GetDailyProgressUseCase useCase;

    private static final Long USER_ID  = 1L;
    private static final LocalDate MONDAY   = LocalDate.of(2026, 4, 20); // Monday
    private static final LocalDate APRIL_25 = LocalDate.of(2026, 4, 25); // Saturday
    private static final LocalDate APRIL_24 = LocalDate.of(2026, 4, 24); // Friday

    @BeforeEach
    void setUp() {
        useCase = new GetDailyProgressUseCase(routineJpaRepository, areaJpaRepository, dailyLogJpaRepository);
    }

    @Test
    @DisplayName("getProgress_twoTasksOneCompleted_returnsCorrectCompletionRate")
    void getProgress_twoTasksOneCompleted_returnsCorrectCompletionRate() {
        var user    = UserJpaEntity.builder().id(USER_ID).build();
        var routine = RoutineJpaEntity.builder().id(1L).user(user).build();

        var task1 = buildDayOfWeekTask(1L, DayOfWeek.MONDAY);
        var task2 = buildDayOfWeekTask(2L, DayOfWeek.MONDAY);
        var area  = buildAreaWithTasks(1L, List.of(task1, task2), user, routine);

        var log1 = DailyLogJpaEntity.builder()
                .task(task1).user(user).logDate(MONDAY)
                .completed(true).completedAt(Instant.now()).build();

        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(routine));
        when(areaJpaRepository.findAreasWithTasksByRoutineIdOrderByOrderIndex(eq(1L)))
                .thenReturn(List.of(area));
        when(dailyLogJpaRepository.findAllByUserIdAndLogDate(USER_ID, MONDAY))
                .thenReturn(List.of(log1));

        var result = useCase.getProgress(USER_ID, MONDAY);

        assertThat(result.logDate()).isEqualTo(MONDAY);
        assertThat(result.areas()).hasSize(1);
        var areaProgress = result.areas().get(0);
        assertThat(areaProgress.totalTasks()).isEqualTo(2);
        assertThat(areaProgress.completedTasks()).isEqualTo(1);
        assertThat(areaProgress.completionRate()).isEqualTo(0.5);
        assertThat(result.overallCompletionRate()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("getProgress_noCheckInsYet_allTasksShowNotCompleted")
    void getProgress_noCheckInsYet_allTasksShowNotCompleted() {
        var user    = UserJpaEntity.builder().id(USER_ID).build();
        var routine = RoutineJpaEntity.builder().id(1L).user(user).build();

        var task = buildDayOfWeekTask(1L, DayOfWeek.MONDAY);
        var area = buildAreaWithTasks(1L, List.of(task), user, routine);

        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(routine));
        when(areaJpaRepository.findAreasWithTasksByRoutineIdOrderByOrderIndex(any()))
                .thenReturn(List.of(area));
        when(dailyLogJpaRepository.findAllByUserIdAndLogDate(USER_ID, MONDAY))
                .thenReturn(List.of());

        var result = useCase.getProgress(USER_ID, MONDAY);

        assertThat(result.areas().get(0).completedTasks()).isEqualTo(0);
        assertThat(result.overallCompletionRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getProgress_noActiveRoutine_returnsEmptyAreas")
    void getProgress_noActiveRoutine_returnsEmptyAreas() {
        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.empty());

        var result = useCase.getProgress(USER_ID, MONDAY);

        assertThat(result.areas()).isEmpty();
        assertThat(result.overallCompletionRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getProgress_dayOfMonth25_onApril25_taskCounted")
    void getProgress_dayOfMonth25_onApril25_taskCounted() {
        var user    = UserJpaEntity.builder().id(USER_ID).build();
        var routine = RoutineJpaEntity.builder().id(1L).user(user).build();

        var task = buildDayOfMonthTask(1L, 25);
        var area = buildAreaWithTasks(1L, List.of(task), user, routine);

        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(routine));
        when(areaJpaRepository.findAreasWithTasksByRoutineIdOrderByOrderIndex(eq(1L)))
                .thenReturn(List.of(area));
        when(dailyLogJpaRepository.findAllByUserIdAndLogDate(USER_ID, APRIL_25))
                .thenReturn(List.of());

        var result = useCase.getProgress(USER_ID, APRIL_25);

        assertThat(result.areas()).hasSize(1);
        assertThat(result.areas().get(0).totalTasks()).isEqualTo(1);
        assertThat(result.areas().get(0).completedTasks()).isEqualTo(0);
    }

    @Test
    @DisplayName("getProgress_dayOfMonth25_onApril24_taskNotCounted")
    void getProgress_dayOfMonth25_onApril24_taskNotCounted() {
        var user    = UserJpaEntity.builder().id(USER_ID).build();
        var routine = RoutineJpaEntity.builder().id(1L).user(user).build();

        var task = buildDayOfMonthTask(1L, 25);
        var area = buildAreaWithTasks(1L, List.of(task), user, routine);

        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(routine));
        when(areaJpaRepository.findAreasWithTasksByRoutineIdOrderByOrderIndex(eq(1L)))
                .thenReturn(List.of(area));
        when(dailyLogJpaRepository.findAllByUserIdAndLogDate(USER_ID, APRIL_24))
                .thenReturn(List.of());

        var result = useCase.getProgress(USER_ID, APRIL_24);

        // DAY_OF_MONTH=25 does not apply on April 24 → area excluded
        assertThat(result.areas()).isEmpty();
        assertThat(result.overallCompletionRate()).isEqualTo(0.0);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private TaskJpaEntity buildDayOfWeekTask(Long id, DayOfWeek day) {
        return TaskJpaEntity.builder()
                .id(id).title("Task " + id)
                .scheduleType(ScheduleType.DAY_OF_WEEK)
                .dayOfWeek(day)
                .orderIndex(0).build();
    }

    private TaskJpaEntity buildDayOfMonthTask(Long id, int dayOfMonth) {
        return TaskJpaEntity.builder()
                .id(id).title("Monthly Task " + id)
                .scheduleType(ScheduleType.DAY_OF_MONTH)
                .dayOfMonth(dayOfMonth)
                .orderIndex(0).build();
    }

    private AreaJpaEntity buildAreaWithTasks(Long id, List<TaskJpaEntity> tasks,
                                              UserJpaEntity user, RoutineJpaEntity routine) {
        var area = AreaJpaEntity.builder()
                .id(id).name("Area " + id).color("#000").icon("🎯")
                .user(user).routine(routine).tasks(new ArrayList<>(tasks)).build();
        tasks.forEach(t -> t.setArea(area));
        return area;
    }
}
