package com.routineflow.infrastructure.persistence.repository;

import com.routineflow.infrastructure.persistence.entity.AreaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.util.List;

public interface AreaJpaRepository extends JpaRepository<AreaJpaEntity, Long> {

    // Busca áreas com tarefas filtradas por dia — evita N+1
    @Query("""
            SELECT DISTINCT a FROM AreaJpaEntity a
            JOIN FETCH a.tasks t
            WHERE a.routine.id = :routineId
              AND t.dayOfWeek = :dayOfWeek
            ORDER BY a.id
            """)
    List<AreaJpaEntity> findAreasWithTasksByRoutineIdAndDay(
            @Param("routineId") Long routineId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek
    );

    List<AreaJpaEntity> findByRoutineId(Long routineId);

    @Query("""
            SELECT DISTINCT a FROM AreaJpaEntity a
            JOIN FETCH a.tasks
            WHERE a.routine.id = :routineId
            ORDER BY a.id
            """)
    List<AreaJpaEntity> findAreasWithAllTasksByRoutineId(@Param("routineId") Long routineId);
}
