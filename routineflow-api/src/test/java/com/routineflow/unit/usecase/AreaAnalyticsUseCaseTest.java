package com.routineflow.unit.usecase;

import com.routineflow.application.usecase.AreaAnalyticsUseCase;
import com.routineflow.application.usecase.exception.ResourceNotFoundException;
import com.routineflow.domain.model.ResetFrequency;
import com.routineflow.infrastructure.persistence.entity.AreaJpaEntity;
import com.routineflow.infrastructure.persistence.entity.StreakJpaEntity;
import com.routineflow.infrastructure.persistence.entity.TaskJpaEntity;
import com.routineflow.infrastructure.persistence.entity.UserJpaEntity;
import com.routineflow.infrastructure.persistence.repository.AreaJpaRepository;
import com.routineflow.infrastructure.persistence.repository.DailyLogJpaRepository;
import com.routineflow.infrastructure.persistence.repository.StreakJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AreaAnalyticsUseCaseTest {

    @Mock private AreaJpaRepository areaJpaRepository;
    @Mock private DailyLogJpaRepository dailyLogJpaRepository;
    @Mock private StreakJpaRepository streakJpaRepository;

    private AreaAnalyticsUseCase useCase;

    private static final Long USER_ID  = 1L;
    private static final Long AREA_ID  = 10L;
    // Area created 90 days ago — guarantees all 12 trend weeks are "after creation"
    private static final Instant AREA_CREATED = Instant.now().minus(90, ChronoUnit.DAYS);

    @BeforeEach
    void setUp() {
        useCase = new AreaAnalyticsUseCase(areaJpaRepository, dailyLogJpaRepository, streakJpaRepository);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAreaAnalytics_areaWithHistory_returnsCompleteResponse")
    void getAreaAnalytics_areaWithHistory_returnsCompleteResponse() {
        var area = buildArea(AREA_ID, USER_ID, AREA_CREATED, DayOfWeek.MONDAY);
        var streak = StreakJpaEntity.builder()
                .area(area).user(area.getUser())
                .currentCount(5).bestStreak(12).build();

        // Last 6 Mondays — arbitrary completed dates
        List<LocalDate> completedDates = recentMondayDates(6);

        when(areaJpaRepository.findByIdAndUserId(AREA_ID, USER_ID)).thenReturn(Optional.of(area));
        when(streakJpaRepository.findByAreaIdAndUserId(AREA_ID, USER_ID)).thenReturn(Optional.of(streak));
        when(dailyLogJpaRepository.countCompletedByAreaId(AREA_ID)).thenReturn(6L);
        when(dailyLogJpaRepository.findCompletedLogDatesByAreaId(AREA_ID)).thenReturn(completedDates);

        var result = useCase.getAreaAnalytics(USER_ID, AREA_ID);

        assertThat(result.areaId()).isEqualTo(AREA_ID);
        assertThat(result.areaName()).isEqualTo("TestArea");
        assertThat(result.currentStreak()).isEqualTo(5);
        assertThat(result.bestStreak()).isEqualTo(12);
        assertThat(result.totalCheckIns()).isEqualTo(6);
        assertThat(result.totalExpected()).isGreaterThan(0);
        assertThat(result.weeklyTrend()).hasSize(12);
        assertThat(result.dayOfWeekStats()).isNotEmpty();
        assertThat(result.overallCompletionRate()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("getAreaAnalytics_areaWithNoCheckIns_returnsAllZeros")
    void getAreaAnalytics_areaWithNoCheckIns_returnsAllZeros() {
        var area = buildArea(AREA_ID, USER_ID, AREA_CREATED, DayOfWeek.MONDAY);

        when(areaJpaRepository.findByIdAndUserId(AREA_ID, USER_ID)).thenReturn(Optional.of(area));
        when(streakJpaRepository.findByAreaIdAndUserId(AREA_ID, USER_ID)).thenReturn(Optional.empty());
        when(dailyLogJpaRepository.countCompletedByAreaId(AREA_ID)).thenReturn(0L);
        when(dailyLogJpaRepository.findCompletedLogDatesByAreaId(AREA_ID)).thenReturn(List.of());

        var result = useCase.getAreaAnalytics(USER_ID, AREA_ID);

        assertThat(result.totalCheckIns()).isZero();
        assertThat(result.currentStreak()).isZero();
        assertThat(result.bestStreak()).isZero();
        assertThat(result.overallCompletionRate()).isEqualTo(0.0);
        assertThat(result.weeklyTrend()).hasSize(12);
        assertThat(result.weeklyTrend()).allMatch(p -> p.completedTasks() == 0);
        assertThat(result.bestDayOfWeek()).isNull();
        assertThat(result.bestDayLabel()).isNull();
    }

    // ── Ownership / not found ─────────────────────────────────────────────────

    @Test
    @DisplayName("getAreaAnalytics_areaNotFound_throwsResourceNotFoundException")
    void getAreaAnalytics_areaNotFound_throwsResourceNotFoundException() {
        when(areaJpaRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getAreaAnalytics(USER_ID, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Area not found");
    }

    @Test
    @DisplayName("getAreaAnalytics_areaOfAnotherUser_throwsResourceNotFoundException")
    void getAreaAnalytics_areaOfAnotherUser_throwsResourceNotFoundException() {
        // findByIdAndUserId returns empty for another user's area (ADR-006)
        when(areaJpaRepository.findByIdAndUserId(AREA_ID, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getAreaAnalytics(99L, AREA_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Weekly trend ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getWeeklyTrend_12WeeksAlwaysReturned_emptiesHaveRateZero")
    void getWeeklyTrend_12WeeksAlwaysReturned_emptiesHaveRateZero() {
        var area = buildArea(AREA_ID, USER_ID, AREA_CREATED, DayOfWeek.MONDAY);

        when(areaJpaRepository.findByIdAndUserId(AREA_ID, USER_ID)).thenReturn(Optional.of(area));
        when(streakJpaRepository.findByAreaIdAndUserId(AREA_ID, USER_ID)).thenReturn(Optional.empty());
        when(dailyLogJpaRepository.countCompletedByAreaId(AREA_ID)).thenReturn(2L);
        // Only 2 completed dates — last 2 Mondays
        when(dailyLogJpaRepository.findCompletedLogDatesByAreaId(AREA_ID))
                .thenReturn(recentMondayDates(2));

        var result = useCase.getAreaAnalytics(USER_ID, AREA_ID);

        assertThat(result.weeklyTrend()).hasSize(12);
        // Total completed across all weeks should be 2
        int totalCompleted = result.weeklyTrend().stream().mapToInt(p -> p.completedTasks()).sum();
        assertThat(totalCompleted).isEqualTo(2);
        // At least some weeks have rate 0
        assertThat(result.weeklyTrend()).anyMatch(p -> p.completionRate() == 0.0);
    }

    // ── Best day of week ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getBestDayOfWeek_mostCheckInsOnWednesday_returnsWednesday")
    void getBestDayOfWeek_mostCheckInsOnWednesday_returnsWednesday() {
        // Area has tasks on MONDAY and WEDNESDAY
        var area = buildAreaWithTwoTasks(AREA_ID, USER_ID, AREA_CREATED,
                DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);

        // 10 Wednesdays completed, 2 Mondays completed → Wednesday should win
        List<LocalDate> completedDates = new java.util.ArrayList<>();
        completedDates.addAll(recentWednesdayDates(10));
        completedDates.addAll(recentMondayDates(2));

        when(areaJpaRepository.findByIdAndUserId(AREA_ID, USER_ID)).thenReturn(Optional.of(area));
        when(streakJpaRepository.findByAreaIdAndUserId(AREA_ID, USER_ID)).thenReturn(Optional.empty());
        when(dailyLogJpaRepository.countCompletedByAreaId(AREA_ID)).thenReturn((long) completedDates.size());
        when(dailyLogJpaRepository.findCompletedLogDatesByAreaId(AREA_ID)).thenReturn(completedDates);

        var result = useCase.getAreaAnalytics(USER_ID, AREA_ID);

        assertThat(result.bestDayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY);
        assertThat(result.bestDayLabel()).isEqualTo("Quarta-feira");
    }

    @Test
    @DisplayName("getBestDayOfWeek_noCheckIns_returnsNull")
    void getBestDayOfWeek_noCheckIns_returnsNull() {
        var area = buildArea(AREA_ID, USER_ID, AREA_CREATED, DayOfWeek.MONDAY);

        when(areaJpaRepository.findByIdAndUserId(AREA_ID, USER_ID)).thenReturn(Optional.of(area));
        when(streakJpaRepository.findByAreaIdAndUserId(AREA_ID, USER_ID)).thenReturn(Optional.empty());
        when(dailyLogJpaRepository.countCompletedByAreaId(AREA_ID)).thenReturn(0L);
        when(dailyLogJpaRepository.findCompletedLogDatesByAreaId(AREA_ID)).thenReturn(List.of());

        var result = useCase.getAreaAnalytics(USER_ID, AREA_ID);

        assertThat(result.bestDayOfWeek()).isNull();
        assertThat(result.bestDayLabel()).isNull();
    }

    @Test
    @DisplayName("getBestDayOfWeek_fewerThan5Samples_returnsNull")
    void getBestDayOfWeek_fewerThan5Samples_returnsNull() {
        var area = buildArea(AREA_ID, USER_ID, AREA_CREATED, DayOfWeek.MONDAY);

        // Only 4 completed Mondays — below the 5-sample threshold
        when(areaJpaRepository.findByIdAndUserId(AREA_ID, USER_ID)).thenReturn(Optional.of(area));
        when(streakJpaRepository.findByAreaIdAndUserId(AREA_ID, USER_ID)).thenReturn(Optional.empty());
        when(dailyLogJpaRepository.countCompletedByAreaId(AREA_ID)).thenReturn(4L);
        when(dailyLogJpaRepository.findCompletedLogDatesByAreaId(AREA_ID))
                .thenReturn(recentMondayDates(4));

        var result = useCase.getAreaAnalytics(USER_ID, AREA_ID);

        assertThat(result.bestDayOfWeek()).isNull();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private AreaJpaEntity buildArea(Long id, Long userId, Instant createdAt, DayOfWeek taskDay) {
        var user = UserJpaEntity.builder().id(userId).build();
        var area = AreaJpaEntity.builder()
                .id(id).name("TestArea").color("#3B82F6").icon("📚")
                .user(user).resetFrequency(ResetFrequency.DAILY).createdAt(createdAt)
                .build();
        var task = TaskJpaEntity.builder()
                .id(100L).title("Task").dayOfWeek(taskDay).orderIndex(0).area(area).build();
        area.getTasks().add(task);
        return area;
    }

    private AreaJpaEntity buildAreaWithTwoTasks(Long id, Long userId, Instant createdAt,
                                                DayOfWeek day1, DayOfWeek day2) {
        var user = UserJpaEntity.builder().id(userId).build();
        var area = AreaJpaEntity.builder()
                .id(id).name("TestArea").color("#3B82F6").icon("📚")
                .user(user).resetFrequency(ResetFrequency.DAILY).createdAt(createdAt)
                .build();
        area.getTasks().add(TaskJpaEntity.builder()
                .id(101L).title("Task1").dayOfWeek(day1).orderIndex(0).area(area).build());
        area.getTasks().add(TaskJpaEntity.builder()
                .id(102L).title("Task2").dayOfWeek(day2).orderIndex(1).area(area).build());
        return area;
    }

    /** Returns the last N past Mondays (most recent first doesn't matter — set is enough). */
    private List<LocalDate> recentMondayDates(int count) {
        List<LocalDate> dates = new java.util.ArrayList<>();
        LocalDate monday = LocalDate.now()
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        for (int i = 0; i < count; i++) {
            dates.add(monday.minusWeeks(i));
        }
        return dates;
    }

    private List<LocalDate> recentWednesdayDates(int count) {
        List<LocalDate> dates = new java.util.ArrayList<>();
        LocalDate wed = LocalDate.now()
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.WEDNESDAY));
        for (int i = 0; i < count; i++) {
            dates.add(wed.minusWeeks(i));
        }
        return dates;
    }
}
