package com.routineflow.unit.usecase;

import com.routineflow.application.dto.CheckInExportRow;
import com.routineflow.application.usecase.ExportUseCase;
import com.routineflow.infrastructure.persistence.repository.DailyLogJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportUseCaseTest {

    @Mock
    private DailyLogJpaRepository dailyLogJpaRepository;

    private ExportUseCase useCase;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        useCase = new ExportUseCase(dailyLogJpaRepository);
    }

    // ── Happy path: user with history ────────────────────────────────────────

    @Test
    @DisplayName("getCheckInsForExport_userWithHistory_returnsCsvRowsWithCorrectData")
    void getCheckInsForExport_userWithHistory_returnsCsvRowsWithCorrectData() {
        LocalDate date = LocalDate.of(2026, 4, 20); // Monday
        Instant completedAt = Instant.parse("2026-04-20T08:30:00Z");

        var row1 = new CheckInExportRow(date, DayOfWeek.MONDAY, "Inglês/PTE", "Re-tell Lecture", true, completedAt);
        var row2 = new CheckInExportRow(date, DayOfWeek.MONDAY, "Inglês/PTE", "Anki", false, null);

        when(dailyLogJpaRepository.findForExport(eq(USER_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(row1, row2));

        List<CheckInExportRow> result = useCase.getCheckInsForExport(USER_ID, null, null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).areaName()).isEqualTo("Inglês/PTE");
        assertThat(result.get(0).taskTitle()).isEqualTo("Re-tell Lecture");
        assertThat(result.get(0).completed()).isTrue();
        assertThat(result.get(0).completedAt()).isEqualTo(completedAt);
        assertThat(result.get(1).completed()).isFalse();
        assertThat(result.get(1).completedAt()).isNull();
    }

    // ── User with no check-ins ────────────────────────────────────────────────

    @Test
    @DisplayName("getCheckInsForExport_userWithNoCheckIns_returnsEmptyList")
    void getCheckInsForExport_userWithNoCheckIns_returnsEmptyList() {
        when(dailyLogJpaRepository.findForExport(eq(USER_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        List<CheckInExportRow> result = useCase.getCheckInsForExport(USER_ID, null, null);

        assertThat(result).isEmpty();
    }

    // ── Filters only authenticated user's data ────────────────────────────────

    @Test
    @DisplayName("getCheckInsForExport_filtersOnlyAuthenticatedUserId")
    void getCheckInsForExport_filtersOnlyAuthenticatedUserId() {
        Long otherUserId = 999L;
        LocalDate date = LocalDate.of(2026, 4, 20);
        var rowForUser = new CheckInExportRow(date, DayOfWeek.MONDAY, "Área A", "Task A", true, null);

        // User 1 gets rows; other user gets none
        when(dailyLogJpaRepository.findForExport(eq(USER_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(rowForUser));
        when(dailyLogJpaRepository.findForExport(eq(otherUserId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        List<CheckInExportRow> user1Result = useCase.getCheckInsForExport(USER_ID, null, null);
        List<CheckInExportRow> otherResult = useCase.getCheckInsForExport(otherUserId, null, null);

        assertThat(user1Result).hasSize(1);
        assertThat(otherResult).isEmpty();
    }

    // ── Date range filter ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getCheckInsForExport_withDateRange_passesRangeToRepository")
    void getCheckInsForExport_withDateRange_passesRangeToRepository() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to   = LocalDate.of(2026, 4, 30);
        LocalDate date = LocalDate.of(2026, 3, 15);
        var row = new CheckInExportRow(date, DayOfWeek.SUNDAY, "Programação", "Projeto", true, null);

        when(dailyLogJpaRepository.findForExport(USER_ID, from, to)).thenReturn(List.of(row));

        List<CheckInExportRow> result = useCase.getCheckInsForExport(USER_ID, from, to);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).logDate()).isEqualTo(date);
        assertThat(result.get(0).taskTitle()).isEqualTo("Projeto");
    }

    // ── Max range validation ──────────────────────────────────────────────────

    @Test
    @DisplayName("getCheckInsForExport_rangeExceeds365Days_throwsIllegalArgumentException")
    void getCheckInsForExport_rangeExceeds365Days_throwsIllegalArgumentException() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to   = LocalDate.of(2026, 4, 30); // > 365 days

        assertThatThrownBy(() -> useCase.getCheckInsForExport(USER_ID, from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("365");
    }
}
