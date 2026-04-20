package com.routineflow.application.usecase;

import com.routineflow.application.dto.AuthResponse;
import com.routineflow.application.dto.LoginRequest;
import com.routineflow.infrastructure.persistence.repository.UserJpaRepository;
import com.routineflow.infrastructure.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;

@Service
public class LoginUseCase {

    private final AuthenticationManager authenticationManager;
    private final UserJpaRepository userJpaRepository;
    private final JwtService jwtService;

    public LoginUseCase(
            AuthenticationManager authenticationManager,
            UserJpaRepository userJpaRepository,
            JwtService jwtService
    ) {
        this.authenticationManager = authenticationManager;
        this.userJpaRepository = userJpaRepository;
        this.jwtService = jwtService;
    }

    public AuthResponse execute(LoginRequest request) {
        // Spring Security valida credenciais e lança BadCredentialsException se inválido
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        var entity = userJpaRepository.findByEmail(request.email())
                .orElseThrow();

        var userDetails = new User(entity.getEmail(), entity.getPasswordHash(), Collections.emptyList());
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(token, entity.getName(), entity.getEmail(), Instant.now().plusMillis(86_400_000));
    }
}
