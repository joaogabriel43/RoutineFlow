package com.routineflow.infrastructure.persistence.repository;

import com.routineflow.infrastructure.persistence.entity.SingleTaskJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SingleTaskJpaRepository extends JpaRepository<SingleTaskJpaEntity, Long> {

    /**
     * Pending tasks for a user — ordered by dueDate ASC NULLS LAST, then createdAt ASC.
     * Hibernate 6 (Spring Boot 3.x) supports NULLS LAST natively in JPQL ORDER BY.
     */
    @Query("""
            SELECT s FROM SingleTaskJpaEntity s
            WHERE s.userId = :userId AND s.completed = false
            ORDER BY s.dueDate ASC NULLS LAST, s.createdAt ASC
            """)
    List<SingleTaskJpaEntity> findPendingByUserId(@Param("userId") Long userId);

    /**
     * Archived (completed) tasks for a user — ordered by completedAt DESC.
     */
    @Query("""
            SELECT s FROM SingleTaskJpaEntity s
            WHERE s.userId = :userId AND s.completed = true
            ORDER BY s.completedAt DESC
            """)
    List<SingleTaskJpaEntity> findArchivedByUserId(@Param("userId") Long userId);
}
