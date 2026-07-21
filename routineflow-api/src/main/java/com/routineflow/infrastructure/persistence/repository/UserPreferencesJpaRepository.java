package com.routineflow.infrastructure.persistence.repository;

import com.routineflow.infrastructure.persistence.entity.UserPreferencesJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPreferencesJpaRepository extends JpaRepository<UserPreferencesJpaEntity, Long> {
    Optional<UserPreferencesJpaEntity> findByUserId(Long userId);
}
