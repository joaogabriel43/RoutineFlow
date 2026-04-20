package com.routineflow.unit.scheduler;

import com.routineflow.domain.service.StreakCalculationService;
import com.routineflow.infrastructure.persistence.entity.RoutineJpaEntity;
import com.routineflow.infrastructure.persistence.entity.UserJpaEntity;
import com.routineflow.infrastructure.persistence.repository.RoutineJpaRepository;
import com.routineflow.infrastructure.persistence.repository.SchedulerRunJpaRepository;
import com.routineflow.infrastructure.scheduler.DailyResetJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyResetJobTest {

    @Mock private StreakCalculationService streakCalculationService;
    @Mock private RoutineJpaRepository routineJpaRepository;
    @Mock private SchedulerRunJpaRepository schedulerRunJpaRepository;

    private DailyResetJob job;

    @BeforeEach
    void setUp() {
        job = new DailyResetJob(streakCalculationService, routineJpaRepository, schedulerRunJpaRepository);
    }

    @Test
    @DisplayName("execute_twoActiveUsers_callsStreakCalculationForEach")
    void execute_twoActiveUsers_callsStreakCalculationForEach() {
        var user1 = UserJpaEntity.builder().id(1L).build();
        var user2 = UserJpaEntity.builder().id(2L).build();
        var routine1 = RoutineJpaEntity.builder().user(user1).active(true).build();
        var routine2 = RoutineJpaEntity.builder().user(user2).active(true).build();

        when(routineJpaRepository.findAllActiveRoutines()).thenReturn(List.of(routine1, routine2));
        when(schedulerRunJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        job.execute();

        verify(streakCalculationService).calculate(eq(1L), any(LocalDate.class));
        verify(streakCalculationService).calculate(eq(2L), any(LocalDate.class));
        verify(schedulerRunJpaRepository, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("execute_noActiveUsers_jobCompletesWithoutCallingStreak")
    void execute_noActiveUsers_jobCompletesWithoutCallingStreak() {
        when(routineJpaRepository.findAllActiveRoutines()).thenReturn(List.of());
        when(schedulerRunJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        job.execute();

        verify(streakCalculationService, never()).calculate(any(), any());
    }

    @Test
    @DisplayName("execute_oneUserThrows_continuesProcessingOtherUsers")
    void execute_oneUserThrows_continuesProcessingOtherUsers() {
        var user1 = UserJpaEntity.builder().id(1L).build();
        var user2 = UserJpaEntity.builder().id(2L).build();
        var routine1 = RoutineJpaEntity.builder().user(user1).active(true).build();
        var routine2 = RoutineJpaEntity.builder().user(user2).active(true).build();

        when(routineJpaRepository.findAllActiveRoutines()).thenReturn(List.of(routine1, routine2));
        when(schedulerRunJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("DB error")).when(streakCalculationService)
                .calculate(eq(1L), any());

        // Não lança — engole o erro do user1 e processa user2
        job.execute();

        verify(streakCalculationService).calculate(eq(2L), any(LocalDate.class));
    }
}
