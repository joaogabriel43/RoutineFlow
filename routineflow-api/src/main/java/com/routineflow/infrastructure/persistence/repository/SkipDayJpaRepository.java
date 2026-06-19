package com.routineflow.infrastructure.persistence.repository;

import com.routineflow.infrastructure.persistence.entity.SkipDayJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SkipDayJpaRepository extends JpaRepository<SkipDayJpaEntity, Long> {

    Optional<SkipDayJpaEntity> findByUserIdAndAreaIdAndSkipDate(Long userId, Long areaId, LocalDate skipDate);

    @Query("SELECT COUNT(s) FROM SkipDayJpaEntity s WHERE s.user.id = :userId AND s.area.id = :areaId AND s.skipDate >= :startOfMonth AND s.skipDate <= :endOfMonth")
    long countByUserIdAndAreaIdAndSkipDateBetween(@Param("userId") Long userId, @Param("areaId") Long areaId, @Param("startOfMonth") LocalDate startOfMonth, @Param("endOfMonth") LocalDate endOfMonth);

    @Query("SELECT s FROM SkipDayJpaEntity s WHERE s.user.id = :userId AND s.skipDate >= :fromDate AND s.skipDate <= :toDate")
    List<SkipDayJpaEntity> findAllByUserIdAndDateRange(@Param("userId") Long userId, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Query("SELECT s FROM SkipDayJpaEntity s WHERE s.user.id = :userId AND s.skipDate = :date")
    List<SkipDayJpaEntity> findAllByUserIdAndSkipDate(@Param("userId") Long userId, @Param("date") LocalDate date);
}
