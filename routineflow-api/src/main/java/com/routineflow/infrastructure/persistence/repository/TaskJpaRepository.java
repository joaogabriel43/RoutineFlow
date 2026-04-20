package com.routineflow.infrastructure.persistence.repository;

import com.routineflow.infrastructure.persistence.entity.TaskJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskJpaRepository extends JpaRepository<TaskJpaEntity, Long> {

    /**
     * Returns rows of [DayOfWeek, Long count] for a given routine.
     * Used by GetHeatmapUseCase to know how many tasks are scheduled per day-of-week.
     */
    @Query("""
            SELECT t.dayOfWeek, COUNT(t)
            FROM TaskJpaEntity t
            WHERE t.area.routine.id = :routineId
            GROUP BY t.dayOfWeek
            """)
    List<Object[]> countByRoutineGroupedByDayOfWeek(@Param("routineId") Long routineId);
}
