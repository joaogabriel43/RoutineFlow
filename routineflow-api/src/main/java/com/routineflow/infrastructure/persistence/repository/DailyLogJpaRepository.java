package com.routineflow.infrastructure.persistence.repository;

import com.routineflow.infrastructure.persistence.entity.DailyLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
}
