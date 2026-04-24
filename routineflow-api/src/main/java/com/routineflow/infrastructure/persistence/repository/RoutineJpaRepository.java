package com.routineflow.infrastructure.persistence.repository;

import com.routineflow.infrastructure.persistence.entity.RoutineJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoutineJpaRepository extends JpaRepository<RoutineJpaEntity, Long> {

    @Modifying
    @Query("UPDATE RoutineJpaEntity r SET r.active = false WHERE r.user.id = :userId AND r.active = true")
    void deactivateAllByUserId(@Param("userId") Long userId);

    @Query("SELECT r FROM RoutineJpaEntity r WHERE r.user.id = :userId AND r.active = true")
    Optional<RoutineJpaEntity> findActiveByUserId(@Param("userId") Long userId);

    @Query("SELECT r FROM RoutineJpaEntity r WHERE r.active = true")
    List<RoutineJpaEntity> findAllActiveRoutines();

    @Query("SELECT r FROM RoutineJpaEntity r WHERE r.user.id = :userId ORDER BY r.importedAt DESC")
    List<RoutineJpaEntity> findAllByUserId(@Param("userId") Long userId);
}
