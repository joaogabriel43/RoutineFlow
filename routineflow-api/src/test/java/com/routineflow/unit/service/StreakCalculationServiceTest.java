package com.routineflow.unit.service;

import com.routineflow.domain.model.ResetFrequency;
import com.routineflow.domain.service.StreakCalculationService;
import com.routineflow.infrastructure.persistence.entity.AreaJpaEntity;
import com.routineflow.infrastructure.persistence.entity.DailyLogJpaEntity;
import com.routineflow.infrastructure.persistence.entity.StreakJpaEntity;
import com.routineflow.infrastructure.persistence.entity.TaskJpaEntity;
import com.routineflow.infrastructure.persistence.entity.UserJpaEntity;
import com.routineflow.infrastructure.persistence.repository.AreaJpaRepository;
import com.routineflow.infrastructure.persistence.repository.DailyLogJpaRepository;
import com.routineflow.infrastructure.persistence.repository.RoutineJpaRepository;
import com.routineflow.infrastructure.persistence.repository.StreakJpaRepository;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StreakCalculationServiceTest {

    @Mock private RoutineJpaRepository routineJpaRepository;
    @Mock private AreaJpaRepository areaJpaRepository;
    @Mock private DailyLogJpaRepository dailyLogJpaRepository;
    @Mock private StreakJpaRepository streakJpaRepository;

    private StreakCalculationService service;

    private static final Long USER_ID = 1L;
    // 2026-04-20 é segunda-feira
    private static final LocalDate MONDAY = LocalDate.of(2026, 4, 20);

    @BeforeEach
    void setUp() {
        service = new StreakCalculationService(
                routineJpaRepository, areaJpaRepository, dailyLogJpaRepository, streakJpaRepository
        );
    }

    @Test
    @DisplayName("calculate_areaWithCompletedTask_incrementsStreak")
    void calculate_areaWithCompletedTask_incrementsStreak() {
        var area = buildAreaWithTask(1L, DayOfWeek.MONDAY);
        var streak = StreakJpaEntity.builder()
                .area(area).user(area.getUser())
                .currentCount(3).lastActiveDate(MONDAY.minusDays(1)).build();

        setupMocks(List.of(area));
        when(dailyLogJpaRepository.findCompletedByUserIdAndAreaIdAndLogDate(USER_ID, 1L, MONDAY))
                .thenReturn(List.of(mock(com.routineflow.infrastructure.persistence.entity.DailyLogJpaEntity.class)));
        when(streakJpaRepository.findByAreaIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(streak));
        when(streakJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.calculate(USER_ID, MONDAY);

        verify(streakJpaRepository).save(argThat(s -> s.getCurrentCount() == 4));
    }

    @Test
    @DisplayName("calculate_currentCountExceedsBestStreak_updatesBestStreak")
    void calculate_currentCountExceedsBestStreak_updatesBestStreak() {
        var area = buildAreaWithTask(1L, DayOfWeek.MONDAY);
        // bestStreak = 5, currentCount = 5 → after increment currentCount=6 > bestStreak=5
        var streak = StreakJpaEntity.builder()
                .area(area).user(area.getUser())
                .currentCount(5).bestStreak(5).lastActiveDate(MONDAY.minusDays(1)).build();

        setupMocks(List.of(area));
        when(dailyLogJpaRepository.findCompletedByUserIdAndAreaIdAndLogDate(USER_ID, 1L, MONDAY))
                .thenReturn(List.of(mock(DailyLogJpaEntity.class)));
        when(streakJpaRepository.findByAreaIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(streak));
        when(streakJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.calculate(USER_ID, MONDAY);

        verify(streakJpaRepository).save(argThat(s ->
                s.getCurrentCount() == 6 && s.getBestStreak() == 6));
    }

    @Test
    @DisplayName("calculate_areaWithNoCompletedTask_resetsStreakToZero")
    void calculate_areaWithNoCompletedTask_resetsStreakToZero() {
        var area = buildAreaWithTask(1L, DayOfWeek.MONDAY);
        var streak = StreakJpaEntity.builder()
                .area(area).user(area.getUser())
                .currentCount(5).lastActiveDate(MONDAY.minusDays(1)).build();

        setupMocks(List.of(area));
        when(dailyLogJpaRepository.findCompletedByUserIdAndAreaIdAndLogDate(USER_ID, 1L, MONDAY))
                .thenReturn(List.of()); // nenhum check-in
        when(streakJpaRepository.findByAreaIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(streak));
        when(streakJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.calculate(USER_ID, MONDAY);

        verify(streakJpaRepository).save(argThat(s -> s.getCurrentCount() == 0));
    }

    @Test
    @DisplayName("calculate_areaWithNoTasksOnDay_streakUnchanged")
    void calculate_areaWithNoTasksOnDay_streakUnchanged() {
        // Área com tarefa na TERÇA — chamamos o job com SEGUNDA
        var area = buildAreaWithTask(1L, DayOfWeek.TUESDAY);

        setupMocks(List.of(area));

        service.calculate(USER_ID, MONDAY);

        // Nenhum save deve ocorrer — dia não conta para essa área
        verify(streakJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("calculate_firstDayOfUse_createsStreakWithCountOne")
    void calculate_firstDayOfUse_createsStreakWithCountOne() {
        var area = buildAreaWithTask(1L, DayOfWeek.MONDAY);

        setupMocks(List.of(area));
        when(dailyLogJpaRepository.findCompletedByUserIdAndAreaIdAndLogDate(USER_ID, 1L, MONDAY))
                .thenReturn(List.of(mock(com.routineflow.infrastructure.persistence.entity.DailyLogJpaEntity.class)));
        when(streakJpaRepository.findByAreaIdAndUserId(1L, USER_ID)).thenReturn(Optional.empty());
        when(streakJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.calculate(USER_ID, MONDAY);

        verify(streakJpaRepository).save(argThat(s -> s.getCurrentCount() == 1));
    }

    // ── WEEKLY frequency ─────────────────────────────────────────────────────

    @Test
    @DisplayName("calculate_weeklyArea_onNonMonday_doesNotEvaluate")
    void calculate_weeklyArea_onNonMonday_doesNotEvaluate() {
        // 2026-04-21 é terça-feira — área WEEKLY não deve ser avaliada
        var tuesday = LocalDate.of(2026, 4, 21);
        var area = buildAreaWithTaskAndFrequency(1L, DayOfWeek.TUESDAY, ResetFrequency.WEEKLY);

        var routine = com.routineflow.infrastructure.persistence.entity.RoutineJpaEntity.builder()
                .id(1L).active(true).build();
        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(routine));
        when(areaJpaRepository.findAreasWithTasksByRoutineIdAndDay(eq(1L), eq(DayOfWeek.TUESDAY)))
                .thenReturn(List.of(area));

        service.calculate(USER_ID, tuesday);

        verify(streakJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("calculate_weeklyArea_onMonday_evaluatesAndIncrementsStreak")
    void calculate_weeklyArea_onMonday_evaluatesAndIncrementsStreak() {
        var area = buildAreaWithTaskAndFrequency(1L, DayOfWeek.MONDAY, ResetFrequency.WEEKLY);

        setupMocks(List.of(area));
        when(dailyLogJpaRepository.findCompletedByUserIdAndAreaIdAndLogDate(USER_ID, 1L, MONDAY))
                .thenReturn(List.of(mock(DailyLogJpaEntity.class)));
        when(streakJpaRepository.findByAreaIdAndUserId(1L, USER_ID)).thenReturn(Optional.empty());
        when(streakJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.calculate(USER_ID, MONDAY);

        verify(streakJpaRepository).save(argThat(s -> s.getCurrentCount() == 1));
    }

    // ── MONTHLY frequency ────────────────────────────────────────────────────

    @Test
    @DisplayName("calculate_monthlyArea_onNonFirstDay_doesNotEvaluate")
    void calculate_monthlyArea_onNonFirstDay_doesNotEvaluate() {
        // 2026-04-20 é o dia 20 — área MONTHLY não deve ser avaliada
        var area = buildAreaWithTaskAndFrequency(1L, DayOfWeek.MONDAY, ResetFrequency.MONTHLY);

        setupMocks(List.of(area));

        service.calculate(USER_ID, MONDAY);

        verify(streakJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("calculate_monthlyArea_onFirstOfMonth_evaluatesAndIncrementsStreak")
    void calculate_monthlyArea_onFirstOfMonth_evaluatesAndIncrementsStreak() {
        // 2026-04-01 é quarta-feira e o 1º do mês
        var firstOfMonth = LocalDate.of(2026, 4, 1);
        var area = buildAreaWithTaskAndFrequency(1L, DayOfWeek.WEDNESDAY, ResetFrequency.MONTHLY);

        var routine = com.routineflow.infrastructure.persistence.entity.RoutineJpaEntity.builder()
                .id(1L).active(true).build();
        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(routine));
        when(areaJpaRepository.findAreasWithTasksByRoutineIdAndDay(eq(1L), eq(DayOfWeek.WEDNESDAY)))
                .thenReturn(List.of(area));
        when(dailyLogJpaRepository.findCompletedByUserIdAndAreaIdAndLogDate(USER_ID, 1L, firstOfMonth))
                .thenReturn(List.of(mock(DailyLogJpaEntity.class)));
        when(streakJpaRepository.findByAreaIdAndUserId(1L, USER_ID)).thenReturn(Optional.empty());
        when(streakJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.calculate(USER_ID, firstOfMonth);

        verify(streakJpaRepository).save(argThat(s -> s.getCurrentCount() == 1));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void setupMocks(List<AreaJpaEntity> areas) {
        var routine = com.routineflow.infrastructure.persistence.entity.RoutineJpaEntity.builder()
                .id(1L).active(true).build();
        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(routine));
        when(areaJpaRepository.findAreasWithTasksByRoutineIdAndDay(eq(1L), eq(DayOfWeek.MONDAY)))
                .thenReturn(areas);
    }

    private AreaJpaEntity buildAreaWithTask(Long areaId, DayOfWeek day) {
        return buildAreaWithTaskAndFrequency(areaId, day, ResetFrequency.DAILY);
    }

    private AreaJpaEntity buildAreaWithTaskAndFrequency(Long areaId, DayOfWeek day, ResetFrequency frequency) {
        var user = UserJpaEntity.builder().id(USER_ID).build();
        var area = AreaJpaEntity.builder().id(areaId).name("Area").color("#000").icon("🎯")
                .user(user).resetFrequency(frequency).build();
        var task = TaskJpaEntity.builder().id(areaId * 10).title("Task")
                .dayOfWeek(day).orderIndex(0).area(area).build();
        area.getTasks().add(task);
        return area;
    }
}
