package com.routineflow.unit.usecase;

import com.routineflow.application.usecase.GetDayScheduleUseCase;
import com.routineflow.domain.model.ScheduleType;
import com.routineflow.infrastructure.persistence.entity.AreaJpaEntity;
import com.routineflow.infrastructure.persistence.entity.RoutineJpaEntity;
import com.routineflow.infrastructure.persistence.entity.TaskJpaEntity;
import com.routineflow.infrastructure.persistence.entity.UserJpaEntity;
import com.routineflow.infrastructure.persistence.repository.AreaJpaRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Tests taskAppliesOnDate() logic in GetDayScheduleUseCase.
 *
 * Reference dates:
 *   2026-04-25 = Saturday
 *   2026-04-20 = Monday
 *   2026-02-28 = last day of February 2026 (not a leap year)
 */
@ExtendWith(MockitoExtension.class)
class GetDayScheduleUseCaseTest {

    @Mock private RoutineJpaRepository routineJpaRepository;
    @Mock private AreaJpaRepository areaJpaRepository;

    private GetDayScheduleUseCase useCase;

    private static final Long USER_ID = 1L;
    private static final LocalDate APRIL_25 = LocalDate.of(2026, 4, 25); // Saturday
    private static final LocalDate APRIL_24 = LocalDate.of(2026, 4, 24); // Friday
    private static final LocalDate FEB_1     = LocalDate.of(2026, 2, 1);  // Feb 1 (not leap)

    @BeforeEach
    void setUp() {
        useCase = new GetDayScheduleUseCase(routineJpaRepository, areaJpaRepository);
    }

    @Test
    @DisplayName("execute_dayOfMonth25_onApril25_taskAppearsInSchedule")
    void execute_dayOfMonth25_onApril25_taskAppearsInSchedule() {
        var user = UserJpaEntity.builder().id(USER_ID).build();
        var routine = RoutineJpaEntity.builder().id(1L).user(user).build();

        var task = buildTask(1L, ScheduleType.DAY_OF_MONTH, null, 25);
        var area = buildAreaWithTasks(1L, List.of(task), user, routine);

        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(routine));
        when(areaJpaRepository.findAreasWithTasksByRoutineIdOrderByOrderIndex(eq(1L)))
                .thenReturn(List.of(area));

        var result = useCase.execute(USER_ID, APRIL_25);

        assertThat(result.areas()).hasSize(1);
        assertThat(result.areas().get(0).tasks()).hasSize(1);
        assertThat(result.areas().get(0).tasks().get(0).dayOfMonth()).isEqualTo(25);
    }

    @Test
    @DisplayName("execute_dayOfMonth25_onApril24_taskDoesNotAppear")
    void execute_dayOfMonth25_onApril24_taskDoesNotAppear() {
        var user = UserJpaEntity.builder().id(USER_ID).build();
        var routine = RoutineJpaEntity.builder().id(1L).user(user).build();

        var task = buildTask(1L, ScheduleType.DAY_OF_MONTH, null, 25);
        var area = buildAreaWithTasks(1L, List.of(task), user, routine);

        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(routine));
        when(areaJpaRepository.findAreasWithTasksByRoutineIdOrderByOrderIndex(eq(1L)))
                .thenReturn(List.of(area));

        var result = useCase.execute(USER_ID, APRIL_24); // 24th, not 25th

        // Area has no applicable tasks → excluded
        assertThat(result.areas()).isEmpty();
    }

    @Test
    @DisplayName("execute_dayOfWeekMonday_onApril25Saturday_taskDoesNotAppear")
    void execute_dayOfWeekMonday_onApril25Saturday_taskDoesNotAppear() {
        var user = UserJpaEntity.builder().id(USER_ID).build();
        var routine = RoutineJpaEntity.builder().id(1L).user(user).build();

        // April 25, 2026 is Saturday — Monday task must NOT appear
        var task = buildTask(1L, ScheduleType.DAY_OF_WEEK, DayOfWeek.MONDAY, null);
        var area = buildAreaWithTasks(1L, List.of(task), user, routine);

        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(routine));
        when(areaJpaRepository.findAreasWithTasksByRoutineIdOrderByOrderIndex(eq(1L)))
                .thenReturn(List.of(area));

        var result = useCase.execute(USER_ID, APRIL_25);

        assertThat(result.areas()).isEmpty();
    }

    @Test
    @DisplayName("execute_dayOfMonth31_inFebruary_taskDoesNotAppear")
    void execute_dayOfMonth31_inFebruary_taskDoesNotAppear() {
        var user = UserJpaEntity.builder().id(USER_ID).build();
        var routine = RoutineJpaEntity.builder().id(1L).user(user).build();

        // February 2026 has 28 days — day 31 must never appear
        var task = buildTask(1L, ScheduleType.DAY_OF_MONTH, null, 31);
        var area = buildAreaWithTasks(1L, List.of(task), user, routine);

        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(routine));
        when(areaJpaRepository.findAreasWithTasksByRoutineIdOrderByOrderIndex(eq(1L)))
                .thenReturn(List.of(area));

        var result = useCase.execute(USER_ID, FEB_1);

        assertThat(result.areas()).isEmpty();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private TaskJpaEntity buildTask(Long id, ScheduleType scheduleType,
                                    DayOfWeek dayOfWeek, Integer dayOfMonth) {
        return TaskJpaEntity.builder()
                .id(id).title("Task " + id)
                .scheduleType(scheduleType)
                .dayOfWeek(dayOfWeek)
                .dayOfMonth(dayOfMonth)
                .orderIndex(0)
                .build();
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
