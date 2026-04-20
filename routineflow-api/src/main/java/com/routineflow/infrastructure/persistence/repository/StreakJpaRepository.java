package com.routineflow.infrastructure.persistence.repository;

import com.routineflow.infrastructure.persistence.entity.StreakJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StreakJpaRepository extends JpaRepository<StreakJpaEntity, Long> {

    @Query("SELECT s FROM StreakJpaEntity s WHERE s.area.id = :areaId AND s.user.id = :userId")
    Optional<StreakJpaEntity> findByAreaIdAndUserId(
            @Param("areaId") Long areaId,
            @Param("userId") Long userId
    );

    @Query("SELECT s FROM StreakJpaEntity s JOIN FETCH s.area WHERE s.user.id = :userId")
    List<StreakJpaEntity> findAllByUserId(@Param("userId") Long userId);
}
