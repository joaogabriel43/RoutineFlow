package com.routineflow.application.usecase;

import com.routineflow.application.dto.AreaAnalyticsResponse;
import com.routineflow.application.dto.DayOfWeekStat;
import com.routineflow.application.dto.WeeklyTrendPoint;
import com.routineflow.application.usecase.exception.ResourceNotFoundException;
import com.routineflow.infrastructure.persistence.entity.AreaJpaEntity;
import com.routineflow.infrastructure.persistence.entity.TaskJpaEntity;
import com.routineflow.infrastructure.persistence.repository.AreaJpaRepository;
import com.routineflow.infrastructure.persistence.repository.DailyLogJpaRepository;
import com.routineflow.infrastructure.persistence.repository.StreakJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AreaAnalyticsUseCase {

    private final AreaJpaRepository areaJpaRepository;
    private final DailyLogJpaRepository dailyLogJpaRepository;
    private final StreakJpaRepository streakJpaRepository;

    public AreaAnalyticsUseCase(
            AreaJpaRepository areaJpaRepository,
            DailyLogJpaRepository dailyLogJpaRepository,
            StreakJpaRepository streakJpaRepository
    ) {
        this.areaJpaRepository = areaJpaRepository;
        this.dailyLogJpaRepository = dailyLogJpaRepository;
        this.streakJpaRepository = streakJpaRepository;
    }

    @Transactional(readOnly = true)
    public AreaAnalyticsResponse getAreaAnalytics(Long userId, Long areaId) {
        // Validate ownership — returns 404 for non-existent OR another user's area (ADR-006)
        var area = areaJpaRepository.findByIdAndUserId(areaId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Area not found: " + areaId));

        // Streak: currentStreak and bestStreak
        var streakOpt = streakJpaRepository.findByAreaIdAndUserId(areaId, userId);
        int currentStreak = streakOpt.map(s -> s.getCurrentCount()).orElse(0);
        int bestStreak    = streakOpt.map(s -> s.getBestStreak()).orElse(0);

        // Total completed check-ins (single COUNT query)
        long totalCheckInsLong = dailyLogJpaRepository.countCompletedByAreaId(areaId);
        int totalCheckIns = (int) totalCheckInsLong;

        // Total expected (calculated in Java — more testable, avoids complex SQL)
        int totalExpected = calculateTotalExpected(area);

        double overallRate = totalExpected == 0 ? 0.0
                : round1dp((double) totalCheckIns / totalExpected * 100.0);

        // Fetch all completed log dates once — reused for weekly trend and DOW stats
        List<LocalDate> completedDates = dailyLogJpaRepository.findCompletedLogDatesByAreaId(areaId);

        // Weekly trend — last 12 weeks, always 12 points
        List<WeeklyTrendPoint> weeklyTrend = buildWeeklyTrend(area, completedDates);

        // Day-of-week breakdown
        List<DayOfWeekStat> dayStats = buildDayOfWeekStats(area, completedDates);

        // Best day (min 5 samples required for statistical significance)
        DayOfWeek bestDow = findBestDayOfWeek(dayStats);

        return new AreaAnalyticsResponse(
                area.getId(),
                area.getName(),
                area.getColor(),
                area.getIcon(),
                area.getResetFrequency(),
                totalCheckIns,
                totalExpected,
                overallRate,
                currentStreak,
                bestStreak,
                weeklyTrend,
                dayStats,
                bestDow,
                bestDow != null ? fullDayLabel(bestDow) : null
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Counts how many times each task was scheduled since the area was created.
     * Each task has one dayOfWeek, so each MONDAY task contributes +1 per elapsed Monday.
     */
    private int calculateTotalExpected(AreaJpaEntity area) {
        List<TaskJpaEntity> tasks = area.getTasks();
        if (tasks.isEmpty()) return 0;

        LocalDate areaCreated = area.getCreatedAt() != null
                ? area.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate()
                : LocalDate.now();
        LocalDate today = LocalDate.now();

        if (!areaCreated.isBefore(today)) return 0;

        int total = 0;
        for (TaskJpaEntity task : tasks) {
            DayOfWeek taskDay = task.getDayOfWeek();
            long count = countDayOccurrences(areaCreated, today, taskDay);
            total += (int) count;
        }
        return total;
    }

    /**
     * Counts how many times dayOfWeek appeared in [from, to] inclusive.
     */
    long countDayOccurrences(LocalDate from, LocalDate to, DayOfWeek day) {
        // Advance `from` to the first occurrence of `day`
        LocalDate first = from;
        while (first.getDayOfWeek() != day) first = first.plusDays(1);
        if (first.isAfter(to)) return 0;
        return ChronoUnit.WEEKS.between(first, to) + 1;
    }

    /**
     * Builds 12 weekly trend points, always in ascending chronological order.
     * Each point covers Mon–Sun. Weeks before area creation have totalTasks=0.
     */
    private List<WeeklyTrendPoint> buildWeeklyTrend(AreaJpaEntity area, List<LocalDate> completedDates) {
        LocalDate today = LocalDate.now();
        LocalDate thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int tasksPerWeek = area.getTasks().size();

        LocalDate areaCreated = area.getCreatedAt() != null
                ? area.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate()
                : thisMonday;

        Set<LocalDate> completedSet = new HashSet<>(completedDates);

        List<WeeklyTrendPoint> trend = new ArrayList<>(12);
        for (int i = 11; i >= 0; i--) {
            LocalDate weekStart = thisMonday.minusWeeks(i);
            LocalDate weekEnd   = weekStart.plusDays(6);

            // Area didn't exist yet → zero
            if (areaCreated.isAfter(weekEnd)) {
                trend.add(new WeeklyTrendPoint(weekStart, formatWeekLabel(weekStart), 0, 0, 0.0));
                continue;
            }

            // Count completed dates that fall in this week
            long completed = completedDates.stream()
                    .filter(d -> !d.isBefore(weekStart) && !d.isAfter(weekEnd))
                    .count();

            int total = tasksPerWeek;
            double rate = total == 0 ? 0.0 : round1dp((double) completed / total * 100.0);

            trend.add(new WeeklyTrendPoint(weekStart, formatWeekLabel(weekStart),
                    (int) completed, total, rate));
        }
        return trend;
    }

    /**
     * Groups completed log dates by day-of-week and computes stats for each scheduled day.
     * Only days that have at least one task scheduled in this area are included.
     */
    private List<DayOfWeekStat> buildDayOfWeekStats(AreaJpaEntity area, List<LocalDate> completedDates) {
        List<TaskJpaEntity> tasks = area.getTasks();
        if (tasks.isEmpty()) return List.of();

        Set<DayOfWeek> scheduledDays = tasks.stream()
                .map(TaskJpaEntity::getDayOfWeek)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));

        Map<DayOfWeek, Long> completedByDay = completedDates.stream()
                .collect(Collectors.groupingBy(LocalDate::getDayOfWeek, Collectors.counting()));

        LocalDate areaCreated = area.getCreatedAt() != null
                ? area.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate()
                : LocalDate.now();
        LocalDate today = LocalDate.now();

        List<DayOfWeekStat> stats = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) { // MONDAY first, per ISO
            if (!scheduledDays.contains(day)) continue;

            long opportunities = countDayOccurrences(areaCreated, today, day);
            long completed     = completedByDay.getOrDefault(day, 0L);
            double rate = opportunities == 0 ? 0.0
                    : round1dp((double) completed / opportunities * 100.0);

            stats.add(new DayOfWeekStat(day, shortDayLabel(day), (int) completed, rate));
        }
        return stats;
    }

    /**
     * Returns the day of week with the highest completionRate that has at least 5 samples.
     * Returns null if no day qualifies.
     */
    private DayOfWeek findBestDayOfWeek(List<DayOfWeekStat> stats) {
        return stats.stream()
                .filter(s -> s.completedCount() >= 5)
                .max(Comparator.comparingDouble(DayOfWeekStat::completionRate))
                .map(DayOfWeekStat::dayOfWeek)
                .orElse(null);
    }

    private static double round1dp(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static String formatWeekLabel(LocalDate weekStart) {
        String[] months = {"jan","fev","mar","abr","mai","jun","jul","ago","set","out","nov","dez"};
        return "Sem " + weekStart.getDayOfMonth() + "/" + months[weekStart.getMonthValue() - 1];
    }

    private static String shortDayLabel(DayOfWeek day) {
        return switch (day) {
            case MONDAY    -> "Segunda";
            case TUESDAY   -> "Terça";
            case WEDNESDAY -> "Quarta";
            case THURSDAY  -> "Quinta";
            case FRIDAY    -> "Sexta";
            case SATURDAY  -> "Sábado";
            case SUNDAY    -> "Domingo";
        };
    }

    private static String fullDayLabel(DayOfWeek day) {
        return switch (day) {
            case MONDAY    -> "Segunda-feira";
            case TUESDAY   -> "Terça-feira";
            case WEDNESDAY -> "Quarta-feira";
            case THURSDAY  -> "Quinta-feira";
            case FRIDAY    -> "Sexta-feira";
            case SATURDAY  -> "Sábado";
            case SUNDAY    -> "Domingo";
        };
    }
}
