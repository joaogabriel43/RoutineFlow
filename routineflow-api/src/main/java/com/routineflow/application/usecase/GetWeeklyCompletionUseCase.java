package com.routineflow.application.usecase;

import com.routineflow.application.dto.WeeklyAreaCompletion;
import com.routineflow.application.dto.WeeklyCompletionResponse;
import com.routineflow.infrastructure.persistence.repository.AreaJpaRepository;
import com.routineflow.infrastructure.persistence.repository.DailyLogJpaRepository;
import com.routineflow.infrastructure.persistence.repository.RoutineJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GetWeeklyCompletionUseCase {

    private final RoutineJpaRepository routineJpaRepository;
    private final AreaJpaRepository areaJpaRepository;
    private final DailyLogJpaRepository dailyLogJpaRepository;

    public GetWeeklyCompletionUseCase(RoutineJpaRepository routineJpaRepository,
                                       AreaJpaRepository areaJpaRepository,
                                       DailyLogJpaRepository dailyLogJpaRepository) {
        this.routineJpaRepository = routineJpaRepository;
        this.areaJpaRepository = areaJpaRepository;
        this.dailyLogJpaRepository = dailyLogJpaRepository;
    }

    @Transactional(readOnly = true)
    public WeeklyCompletionResponse getWeeklyCompletion(Long userId, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);

        var routineOpt = routineJpaRepository.findActiveByUserId(userId);
        if (routineOpt.isEmpty()) {
            return new WeeklyCompletionResponse(weekStart, weekEnd, List.of(), 0.0);
        }

        var routine = routineOpt.get();
        var areas = areaJpaRepository.findAreasWithAllTasksByRoutineId(routine.getId());

        Map<Long, Integer> completionsPerArea = buildCompletionsPerArea(userId, weekStart, weekEnd);

        List<WeeklyAreaCompletion> areaCompletions = areas.stream()
                .map(area -> {
                    int totalTasks = area.getTasks().size();
                    int completedTasks = completionsPerArea.getOrDefault(area.getId(), 0);
                    double completionRate = totalTasks > 0 ? (double) completedTasks / totalTasks : 0.0;
                    return new WeeklyAreaCompletion(
                            area.getId(), area.getName(), area.getColor(), area.getIcon(),
                            completedTasks, totalTasks, completionRate
                    );
                })
                .collect(Collectors.toList());

        double overallRate = computeOverallRate(areaCompletions);
        return new WeeklyCompletionResponse(weekStart, weekEnd, areaCompletions, overallRate);
    }

    private Map<Long, Integer> buildCompletionsPerArea(Long userId, LocalDate from, LocalDate to) {
        Map<Long, Integer> result = new HashMap<>();
        for (Object[] row : dailyLogJpaRepository.countCompletedByUserIdAndAreaGroupedByArea(userId, from, to)) {
            Long areaId = (Long) row[0];
            int count = ((Long) row[1]).intValue();
            result.put(areaId, count);
        }
        return result;
    }

    private double computeOverallRate(List<WeeklyAreaCompletion> areas) {
        int totalTasks = areas.stream().mapToInt(WeeklyAreaCompletion::totalTasks).sum();
        if (totalTasks == 0) return 0.0;
        int totalCompleted = areas.stream().mapToInt(WeeklyAreaCompletion::completedTasks).sum();
        return (double) totalCompleted / totalTasks;
    }
}
