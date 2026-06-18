package com.routineflow.infrastructure.persistence.repository;

import com.routineflow.infrastructure.persistence.entity.PushSubscriptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushSubscriptionJpaRepository extends JpaRepository<PushSubscriptionJpaEntity, Long> {

    List<PushSubscriptionJpaEntity> findAllByUserId(Long userId);

    Optional<PushSubscriptionJpaEntity> findByEndpoint(String endpoint);

    void deleteByEndpoint(String endpoint);
}
