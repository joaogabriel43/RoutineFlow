package com.routineflow.infrastructure.persistence.repository;

import com.routineflow.infrastructure.persistence.entity.SchedulerRunJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchedulerRunJpaRepository extends JpaRepository<SchedulerRunJpaEntity, Long> {}
