package com.routineflow.domain.service;

import com.routineflow.infrastructure.persistence.entity.AreaJpaEntity;
import com.routineflow.infrastructure.persistence.entity.StreakJpaEntity;
import com.routineflow.infrastructure.persistence.repository.AreaJpaRepository;
import com.routineflow.infrastructure.persistence.repository.DailyLogJpaRepository;
import com.routineflow.infrastructure.persistence.repository.RoutineJpaRepository;
import com.routineflow.infrastructure.persistence.repository.StreakJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Calcula e persiste o streak de cada área para um usuário em uma data específica.
 * Regras: área sem tarefas no dia → sem alteração.
 *         pelo menos 1 check-in completed → incrementa.
 *         nenhum check-in → zera.
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

        var dayOfWeek = date.getDayOfWeek();
        List<AreaJpaEntity> areas = areaJpaRepository
                .findAreasWithTasksByRoutineIdAndDay(routineOpt.get().getId(), dayOfWeek);

        for (AreaJpaEntity area : areas) {
            boolean hasTasksToday = area.getTasks().stream()
                    .anyMatch(t -> t.getDayOfWeek() == dayOfWeek);

            if (!hasTasksToday) continue; // não conta para o streak

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
            } else {
                streak.setCurrentCount(0);
            }
            streak.setLastActiveDate(date);
            streakJpaRepository.save(streak);
        }
    }
}
