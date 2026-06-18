package com.routineflow.infrastructure.persistence.repository;

import com.routineflow.infrastructure.persistence.entity.TaskJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

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

    // Busca tasks de uma área ordenadas por orderIndex — para determinar próximo índice e exibição
    List<TaskJpaEntity> findByAreaIdOrderByOrderIndex(Long areaId);

    // Busca por id + userId para validação de ownership em operações de escrita
    // Resolve para: WHERE t.id = :id AND t.area.user.id = :userId
    Optional<TaskJpaEntity> findByIdAndArea_User_Id(Long id, Long userId);

    /**
     * Finds all tasks with a specific reminderTime, eagerly loading area and area.user
     * so the DailyReminderJob can access the owner without extra queries.
     */
    @Query("""
            SELECT t FROM TaskJpaEntity t
            JOIN FETCH t.area a
            JOIN FETCH a.user
            WHERE t.reminderTime = :reminderTime
            """)
    List<TaskJpaEntity> findByReminderTime(@Param("reminderTime") LocalTime reminderTime);
}
