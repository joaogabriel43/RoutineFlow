package com.routineflow.infrastructure.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.time.Instant;
import java.util.Collection;

public class CustomUserDetails extends User {
    private final Long id;
    private final Instant tokensRevokedBefore;

    public CustomUserDetails(String username, String password, Collection<? extends GrantedAuthority> authorities, Long id, Instant tokensRevokedBefore) {
        super(username, password, authorities);
        this.id = id;
        this.tokensRevokedBefore = tokensRevokedBefore;
    }

    public Long getId() {
        return id;
    }

    public Instant getTokensRevokedBefore() {
        return tokensRevokedBefore;
    }
}
