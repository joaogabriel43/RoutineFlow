package com.routineflow.application.usecase;

import com.routineflow.application.dto.CheckInExportRow;
import com.routineflow.infrastructure.config.AppTimeZone;
import com.routineflow.infrastructure.persistence.repository.DailyLogJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ExportUseCase {

    private static final int MAX_RANGE_DAYS    = 365;
    private static final int DEFAULT_RANGE_DAYS = 90;

    private final DailyLogJpaRepository dailyLogJpaRepository;

    public ExportUseCase(DailyLogJpaRepository dailyLogJpaRepository) {
        this.dailyLogJpaRepository = dailyLogJpaRepository;
    }

    @Transactional(readOnly = true)
    public List<CheckInExportRow> getCheckInsForExport(Long userId, LocalDate from, LocalDate to) {
        LocalDate effectiveTo   = to   != null ? to   : LocalDate.now(AppTimeZone.ZONE);
        LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(DEFAULT_RANGE_DAYS);

        long rangeDays = effectiveFrom.until(effectiveTo, java.time.temporal.ChronoUnit.DAYS);
        if (rangeDays > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException(
                    "Export range cannot exceed " + MAX_RANGE_DAYS + " days. Requested: " + rangeDays + " days.");
        }

        return dailyLogJpaRepository.findForExport(userId, effectiveFrom, effectiveTo);
    }
}
