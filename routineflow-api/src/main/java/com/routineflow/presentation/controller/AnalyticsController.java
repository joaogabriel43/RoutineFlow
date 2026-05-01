package com.routineflow.presentation.controller;

import com.routineflow.application.dto.HeatmapResponse;
import com.routineflow.application.dto.StreakListResponse;
import com.routineflow.application.dto.WeekComparisonResponse;
import com.routineflow.application.dto.WeeklyCompletionResponse;
import com.routineflow.application.dto.WeeklyHistoryResponse;
import com.routineflow.application.usecase.GetHeatmapUseCase;
import com.routineflow.application.usecase.GetStreakUseCase;
import com.routineflow.application.usecase.GetWeekComparisonUseCase;
import com.routineflow.application.usecase.GetWeeklyCompletionUseCase;
import com.routineflow.application.usecase.GetWeeklyHistoryUseCase;
import com.routineflow.infrastructure.config.AppTimeZone;
import com.routineflow.infrastructure.security.AuthenticatedUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Tag(name = "Analytics", description = "Streaks, heatmap, and weekly completion analytics")
@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final GetStreakUseCase getStreakUseCase;
    private final GetHeatmapUseCase getHeatmapUseCase;
    private final GetWeeklyCompletionUseCase getWeeklyCompletionUseCase;
    private final GetWeekComparisonUseCase getWeekComparisonUseCase;
    private final GetWeeklyHistoryUseCase getWeeklyHistoryUseCase;
    private final AuthenticatedUserResolver userResolver;

    public AnalyticsController(
            GetStreakUseCase getStreakUseCase,
            GetHeatmapUseCase getHeatmapUseCase,
            GetWeeklyCompletionUseCase getWeeklyCompletionUseCase,
            GetWeekComparisonUseCase getWeekComparisonUseCase,
            GetWeeklyHistoryUseCase getWeeklyHistoryUseCase,
            AuthenticatedUserResolver userResolver
    ) {
        this.getStreakUseCase = getStreakUseCase;
        this.getHeatmapUseCase = getHeatmapUseCase;
        this.getWeeklyCompletionUseCase = getWeeklyCompletionUseCase;
        this.getWeekComparisonUseCase = getWeekComparisonUseCase;
        this.getWeeklyHistoryUseCase = getWeeklyHistoryUseCase;
        this.userResolver = userResolver;
    }

    @Operation(summary = "Get current streak for all areas")
    @GetMapping("/streaks")
    public ResponseEntity<StreakListResponse> getStreaks() {
        Long userId = userResolver.currentUserId();
        return ResponseEntity.ok(getStreakUseCase.getStreaks(userId));
    }

    /**
     * GET /analytics/heatmap?from=2026-01-01&to=2026-04-19
     * Defaults: to=today, from=90 days before today.
     * Range must be ≤ 365 days; from must not be after to.
     */
    @Operation(summary = "Get completion heatmap (defaults: last 90 days, max 365)")
    @GetMapping("/heatmap")
    public ResponseEntity<HeatmapResponse> getHeatmap(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        Long userId = userResolver.currentUserId();
        LocalDate effectiveTo   = to   != null ? to   : LocalDate.now(AppTimeZone.ZONE);
        LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(89);
        return ResponseEntity.ok(getHeatmapUseCase.getHeatmap(userId, effectiveFrom, effectiveTo));
    }

    /**
     * GET /analytics/weekly/current
     * Returns completion data for the current ISO week (Mon–Sun).
     */
    @GetMapping("/weekly/current")
    public ResponseEntity<WeeklyCompletionResponse> getCurrentWeek() {
        Long userId = userResolver.currentUserId();
        LocalDate weekStart = LocalDate.now(AppTimeZone.ZONE).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return ResponseEntity.ok(getWeeklyCompletionUseCase.getWeeklyCompletion(userId, weekStart));
    }

    /**
     * GET /analytics/weekly/comparison
     * Compares current week vs previous week, with per-area deltas.
     */
    @GetMapping("/weekly/comparison")
    public ResponseEntity<WeekComparisonResponse> getWeekComparison() {
        Long userId = userResolver.currentUserId();
        return ResponseEntity.ok(getWeekComparisonUseCase.getComparison(userId, LocalDate.now(AppTimeZone.ZONE)));
    }

    /**
     * GET /analytics/weekly/history?weeks=8
     * Returns last N weeks of completion data. Default: 8, max: 12.
     */
    @GetMapping("/weekly/history")
    public ResponseEntity<WeeklyHistoryResponse> getWeeklyHistory(
            @RequestParam(defaultValue = "8") int weeks
    ) {
        Long userId = userResolver.currentUserId();
        return ResponseEntity.ok(getWeeklyHistoryUseCase.getHistory(userId, weeks, LocalDate.now(AppTimeZone.ZONE)));
    }
}
