package com.routineflow.application.usecase;

import com.routineflow.application.dto.HeatmapDayResponse;
import com.routineflow.application.dto.HeatmapResponse;
import com.routineflow.infrastructure.persistence.repository.DailyLogJpaRepository;
import com.routineflow.infrastructure.persistence.repository.RoutineJpaRepository;
import com.routineflow.infrastructure.persistence.repository.SkipDayJpaRepository;
import com.routineflow.infrastructure.persistence.repository.TaskJpaRepository;
import com.routineflow.infrastructure.persistence.entity.SkipDayJpaEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GetHeatmapUseCase {

    private final RoutineJpaRepository routineJpaRepository;
    private final TaskJpaRepository taskJpaRepository;
    private final DailyLogJpaRepository dailyLogJpaRepository;
    private final SkipDayJpaRepository skipDayJpaRepository;

    public GetHeatmapUseCase(RoutineJpaRepository routineJpaRepository,
                              TaskJpaRepository taskJpaRepository,
                              DailyLogJpaRepository dailyLogJpaRepository,
                              SkipDayJpaRepository skipDayJpaRepository) {
        this.routineJpaRepository = routineJpaRepository;
        this.taskJpaRepository = taskJpaRepository;
        this.dailyLogJpaRepository = dailyLogJpaRepository;
        this.skipDayJpaRepository = skipDayJpaRepository;
    }

    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(
            value = com.routineflow.infrastructure.config.CacheConfig.CACHE_HEATMAP,
            key = "#userId"
    )
    public HeatmapResponse getHeatmap(Long userId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from date must not be after to date");
        }
        long daysBetween = ChronoUnit.DAYS.between(from, to);
        if (daysBetween > 365) {
            throw new IllegalArgumentException("Date range must not exceed 365 days");
        }

        Map<DayOfWeek, Integer> tasksPerDayOfWeek = buildTasksPerDayOfWeek(userId);
        Map<LocalDate, Integer> completionsPerDate = buildCompletionsPerDate(userId, from, to);
        Set<LocalDate> skipDates = skipDayJpaRepository.findAllByUserIdAndDateRange(userId, from, to)
                .stream()
                .map(SkipDayJpaEntity::getSkipDate)
                .collect(Collectors.toSet());

        List<HeatmapDayResponse> days = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            int totalTasks = tasksPerDayOfWeek.getOrDefault(date.getDayOfWeek(), 0);
            int completedTasks = completionsPerDate.getOrDefault(date, 0);
            double completionRate = totalTasks > 0 ? (double) completedTasks / totalTasks : 0.0;
            boolean hasSkipDay = skipDates.contains(date);
            days.add(new HeatmapDayResponse(date, completedTasks, totalTasks, completionRate, hasSkipDay));
        }

        HeatmapDayResponse peakDay = days.stream()
                .filter(d -> d.completedTasks() > 0)
                .max(Comparator.comparingInt(HeatmapDayResponse::completedTasks))
                .orElse(null);

        return new HeatmapResponse(from, to, days, peakDay);
    }

    private Map<DayOfWeek, Integer> buildTasksPerDayOfWeek(Long userId) {
        Map<DayOfWeek, Integer> result = new EnumMap<>(DayOfWeek.class);
        routineJpaRepository.findActiveByUserId(userId).ifPresent(routine -> {
            for (Object[] row : taskJpaRepository.countByRoutineGroupedByDayOfWeek(routine.getId())) {
                DayOfWeek dow = (DayOfWeek) row[0];
                int count = ((Long) row[1]).intValue();
                result.put(dow, count);
            }
        });
        return result;
    }

    private Map<LocalDate, Integer> buildCompletionsPerDate(Long userId, LocalDate from, LocalDate to) {
        Map<LocalDate, Integer> result = new HashMap<>();
        for (Object[] row : dailyLogJpaRepository.countCompletedByUserIdGroupedByDate(userId, from, to)) {
            LocalDate date = (LocalDate) row[0];
            int count = ((Long) row[1]).intValue();
            result.put(date, count);
        }
        return result;
    }
}
