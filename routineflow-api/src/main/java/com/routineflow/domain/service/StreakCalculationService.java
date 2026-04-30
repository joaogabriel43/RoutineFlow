package com.routineflow.domain.service;

import com.routineflow.application.usecase.GetDayScheduleUseCase;
import com.routineflow.domain.model.ResetFrequency;
import com.routineflow.infrastructure.persistence.entity.AreaJpaEntity;
import com.routineflow.infrastructure.persistence.entity.StreakJpaEntity;
import com.routineflow.infrastructure.persistence.repository.AreaJpaRepository;
import com.routineflow.infrastructure.persistence.repository.DailyLogJpaRepository;
import com.routineflow.infrastructure.persistence.repository.RoutineJpaRepository;
import com.routineflow.infrastructure.persistence.repository.StreakJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * Calcula e persiste o streak de cada área para um usuário em uma data específica.
 *
 * Regras gerais:
 *   - Área sem tarefas no dia → sem alteração.
 *   - shouldEvaluateStreak() == false → sem alteração (frequência não atingida).
 *   - Pelo menos 1 check-in completed → incrementa.
 *   - Nenhum check-in no dia de avaliação → zera.
 *
 * Frequências:
 *   DAILY   — avalia todo dia (comportamento original).
 *   WEEKLY  — avalia apenas na segunda-feira.
 *   MONTHLY — avalia apenas no dia 1 do mês.
 *
 * scheduleType:
 *   DAY_OF_WEEK  — tarefa cíclica por dia da semana (original).
 *   DAY_OF_MONTH — tarefa cíclica por dia do mês (ex: todo dia 25).
 */
@Service
public class StreakCalculationService {

    private final RoutineJpaRepository routineJpaRepository;
    private final AreaJpaRepository areaJpaRepository;
    private final DailyLogJpaRepository dailyLogJpaRepository;
    private final StreakJpaRepository streakJpaRepository;

    public StreakCalculationService(
            RoutineJpaRepository routineJpaRepository,
            AreaJpaRepository areaJpaRepository,
            DailyLogJpaRepository dailyLogJpaRepository,
            StreakJpaRepository streakJpaRepository
    ) {
        this.routineJpaRepository = routineJpaRepository;
        this.areaJpaRepository = areaJpaRepository;
        this.dailyLogJpaRepository = dailyLogJpaRepository;
        this.streakJpaRepository = streakJpaRepository;
    }

    @Transactional
    public void calculate(Long userId, LocalDate date) {
        var routineOpt = routineJpaRepository.findActiveByUserId(userId);
        if (routineOpt.isEmpty()) return;

        // Load all tasks — filter using taskAppliesOnDate to support DAY_OF_MONTH
        List<AreaJpaEntity> areas = areaJpaRepository
                .findAreasWithTasksByRoutineIdOrderByOrderIndex(routineOpt.get().getId());

        for (AreaJpaEntity area : areas) {
            boolean hasTasksToday = area.getTasks().stream()
                    .anyMatch(t -> GetDayScheduleUseCase.taskAppliesOnDate(t, date));

            if (!hasTasksToday) continue;

            // Skip evaluation if this area's frequency hasn't triggered yet.
            // DAILY: always evaluate. WEEKLY: only on Monday. MONTHLY: only on the 1st.
            ResetFrequency frequency = area.getResetFrequency() != null
                    ? area.getResetFrequency()
                    : ResetFrequency.DAILY;

            if (!shouldEvaluateStreak(frequency, date)) continue;

            boolean hasCompletion = !dailyLogJpaRepository
                    .findCompletedByUserIdAndAreaIdAndLogDate(userId, area.getId(), date)
                    .isEmpty();

            var streak = streakJpaRepository.findByAreaIdAndUserId(area.getId(), userId)
                    .orElseGet(() -> StreakJpaEntity.builder()
                            .area(area)
                            .user(area.getUser())
                            .currentCount(0)
                            .build());

            if (hasCompletion) {
                streak.setCurrentCount(streak.getCurrentCount() + 1);
                if (streak.getCurrentCount() > streak.getBestStreak()) {
                    streak.setBestStreak(streak.getCurrentCount());
                }
            } else {
                streak.setCurrentCount(0);
            }
            streak.setLastActiveDate(date);
            streakJpaRepository.save(streak);
        }
    }

    /**
     * Returns true when the area's streak should be evaluated for the given date.
     * DAILY  → always.
     * WEEKLY → only on Monday.
     * MONTHLY → only on the 1st of the month.
     */
    private boolean shouldEvaluateStreak(ResetFrequency frequency, LocalDate date) {
        return switch (frequency) {
            case DAILY -> true;
            case WEEKLY -> date.getDayOfWeek() == DayOfWeek.MONDAY;
            case MONTHLY -> date.getDayOfMonth() == 1;
        };
    }
}
