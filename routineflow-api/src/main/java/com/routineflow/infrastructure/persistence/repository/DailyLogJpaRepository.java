package com.routineflow.infrastructure.persistence.repository;

import com.routineflow.application.dto.CheckInExportRow;
import com.routineflow.infrastructure.persistence.entity.DailyLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Note: avoid DAYOFWEEK() — it is MySQL-only. Use EXTRACT(DOW FROM ...) for PostgreSQL.

public interface DailyLogJpaRepository extends JpaRepository<DailyLogJpaEntity, Long> {

    @Query("""
            SELECT d FROM DailyLogJpaEntity d
            WHERE d.task.id = :taskId
              AND d.user.id = :userId
              AND d.logDate = :logDate
            """)
    Optional<DailyLogJpaEntity> findByTaskIdAndUserIdAndLogDate(
            @Param("taskId") Long taskId,
            @Param("userId") Long userId,
            @Param("logDate") LocalDate logDate
    );

    @Query("""
            SELECT d FROM DailyLogJpaEntity d
            WHERE d.user.id = :userId
              AND d.logDate = :logDate
            """)
    List<DailyLogJpaEntity> findAllByUserIdAndLogDate(
            @Param("userId") Long userId,
            @Param("logDate") LocalDate logDate
    );

    @Query("""
            SELECT d FROM DailyLogJpaEntity d
            WHERE d.user.id = :userId
              AND d.task.area.id = :areaId
              AND d.logDate = :logDate
              AND d.completed = true
            """)
    List<DailyLogJpaEntity> findCompletedByUserIdAndAreaIdAndLogDate(
            @Param("userId") Long userId,
            @Param("areaId") Long areaId,
            @Param("logDate") LocalDate logDate
    );

    /**
     * Returns rows of [LocalDate logDate, Long count] for completed logs in a date range.
     * Used by GetHeatmapUseCase to build completion counts per day.
     */
    @Query("""
            SELECT d.logDate, COUNT(d)
            FROM DailyLogJpaEntity d
            WHERE d.user.id = :userId
              AND d.logDate BETWEEN :from AND :to
              AND d.completed = true
            GROUP BY d.logDate
            """)
    List<Object[]> countCompletedByUserIdGroupedByDate(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    /**
     * Returns rows of [Long areaId, Long count] for completed logs grouped by area in a date range.
     * Used by GetWeeklyCompletionUseCase to compute per-area completion counts for a week.
     */
    @Query("""
            SELECT d.task.area.id, COUNT(d)
            FROM DailyLogJpaEntity d
            WHERE d.user.id = :userId
              AND d.logDate BETWEEN :from AND :to
              AND d.completed = true
            GROUP BY d.task.area.id
            """)
    List<Object[]> countCompletedByUserIdAndAreaGroupedByArea(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    /**
     * Total completed check-ins for all tasks in a given area.
     * Used by AreaAnalyticsUseCase.
     */
    @Query("""
            SELECT COUNT(d)
            FROM DailyLogJpaEntity d
            WHERE d.task.area.id = :areaId AND d.completed = true
            """)
    long countCompletedByAreaId(@Param("areaId") Long areaId);

    /**
     * All completed log dates for a given area (no grouping — grouped in Java to avoid
     * DB-specific date-of-week functions like DAYOFWEEK or EXTRACT(DOW)).
     * Used by AreaAnalyticsUseCase to build day-of-week stats and weekly trend.
     */
    @Query("""
            SELECT d.logDate
            FROM DailyLogJpaEntity d
            WHERE d.task.area.id = :areaId AND d.completed = true
            """)
    List<LocalDate> findCompletedLogDatesByAreaId(@Param("areaId") Long areaId);

    /**
     * Full export query — joins daily_logs → tasks → areas → routines to enforce
     * userId ownership. Returns CheckInExportRow projection records ordered by
     * logDate ASC, areaName ASC, taskTitle ASC.
     */
    @Query("""
            SELECT new com.routineflow.application.dto.CheckInExportRow(
                dl.logDate,
                t.dayOfWeek,
                a.name,
                t.title,
                dl.completed,
                dl.completedAt)
            FROM DailyLogJpaEntity dl
            JOIN TaskJpaEntity t ON dl.task.id = t.id
            JOIN AreaJpaEntity a ON t.area.id = a.id
            JOIN RoutineJpaEntity r ON a.routine.id = r.id
            WHERE r.user.id = :userId
              AND dl.logDate BETWEEN :from AND :to
            ORDER BY dl.logDate ASC, a.name ASC, t.title ASC
            """)
    List<CheckInExportRow> findForExport(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
