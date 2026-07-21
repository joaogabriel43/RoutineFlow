package com.routineflow.application.usecase;

import com.routineflow.infrastructure.persistence.entity.UserJpaEntity;
import com.routineflow.infrastructure.persistence.repository.UserJpaRepository;
import com.routineflow.infrastructure.security.AuthenticatedUserResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RevokeSessionsUseCase {

    private final UserJpaRepository userJpaRepository;
    private final AuthenticatedUserResolver userResolver;

    public RevokeSessionsUseCase(UserJpaRepository userJpaRepository, AuthenticatedUserResolver userResolver) {
        this.userJpaRepository = userJpaRepository;
        this.userResolver = userResolver;
    }

    @Transactional
    public void execute() {
        Long userId = userResolver.currentUserId();
        UserJpaEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setTokensRevokedBefore(Instant.now());
        userJpaRepository.save(user);
    }
}
