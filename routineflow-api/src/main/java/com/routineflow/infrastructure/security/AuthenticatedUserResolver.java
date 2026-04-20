package com.routineflow.infrastructure.security;

import com.routineflow.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserResolver {

    private final UserJpaRepository userJpaRepository;

    public AuthenticatedUserResolver(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    public Long currentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userJpaRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email))
                .getId();
    }
}
