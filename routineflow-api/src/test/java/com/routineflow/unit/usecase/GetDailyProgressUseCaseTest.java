package com.routineflow.unit.usecase;

import com.routineflow.application.usecase.GetDailyProgressUseCase;
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

    private static final Long USER_ID = 1L;
    private static final LocalDate MONDAY = LocalDate.of(2026, 4, 20); // Monday

    @BeforeEach
    void setUp() {
        useCase = new GetDailyProgressUseCase(routineJpaRepository, areaJpaRepository, dailyLogJpaRepository);
    }

    @Test
    @DisplayName("getProgress_twoTasksOneCompleted_returnsCorrectCompletionRate")
    void getProgress_twoTasksOneCompleted_returnsCorrectCompletionRate() {
        var user = UserJpaEntity.builder().id(USER_ID).build();
        var routine = RoutineJpaEntity.builder().id(1L).user(user).build();

        var task1 = buildTask(1L, DayOfWeek.MONDAY);
        var task2 = buildTask(2L, DayOfWeek.MONDAY);
        var area = buildAreaWithTasks(1L, List.of(task1, task2), user, routine);
        task1.setArea(area);
        task2.setArea(area);

        var log1 = DailyLogJpaEntity.builder()
                .task(task1).user(user).logDate(MONDAY)
                .completed(true).completedAt(Instant.now()).build();

        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(routine));
        when(areaJpaRepository.findAreasWithTasksByRoutineIdAndDay(eq(1L), eq(DayOfWeek.MONDAY)))
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
        var user = UserJpaEntity.builder().id(USER_ID).build();
        var routine = RoutineJpaEntity.builder().id(1L).user(user).build();

        var task = buildTask(1L, DayOfWeek.MONDAY);
        var area = buildAreaWithTasks(1L, List.of(task), user, routine);
        task.setArea(area);

        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(routine));
        when(areaJpaRepository.findAreasWithTasksByRoutineIdAndDay(any(), any()))
                .thenReturn(List.of(area));
        when(dailyLogJpaRepository.findAllByUserIdAndLogDate(USER_ID, MONDAY))
                .thenReturn(List.of()); // sem check-ins

        var result = useCase.getProgress(USER_ID, MONDAY);

        assertThat(result.areas().get(0).completedTasks()).isEqualTo(0);
        assertThat(result.areas().get(0).completionRate()).isEqualTo(0.0);
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

    private TaskJpaEntity buildTask(Long id, DayOfWeek day) {
        return TaskJpaEntity.builder().id(id).title("Task " + id)
                .dayOfWeek(day).orderIndex(0).build();
    }

    private AreaJpaEntity buildAreaWithTasks(Long id, List<TaskJpaEntity> tasks,
                                              UserJpaEntity user, RoutineJpaEntity routine) {
        var area = AreaJpaEntity.builder()
                .id(id).name("Area " + id).color("#000").icon("🎯")
                .user(user).routine(routine).tasks(new java.util.ArrayList<>(tasks)).build();
        tasks.forEach(t -> t.setArea(area));
        return area;
    }
}
