package com.routineflow.application.usecase;

import com.routineflow.application.dto.AuthResponse;
import com.routineflow.application.dto.RegisterRequest;
import com.routineflow.infrastructure.persistence.entity.UserJpaEntity;
import com.routineflow.infrastructure.persistence.repository.UserJpaRepository;
import com.routineflow.infrastructure.security.JwtService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;

@Service
public class RegisterUserUseCase {

    private final UserJpaRepository userJpaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public RegisterUserUseCase(
            UserJpaRepository userJpaRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userJpaRepository = userJpaRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse execute(RegisterRequest request) {
        if (userJpaRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyInUseException(request.email());
        }

        var entity = UserJpaEntity.builder()
                .email(request.email())
                .name(request.name())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();

        userJpaRepository.save(entity);

        var userDetails = new User(entity.getEmail(), entity.getPasswordHash(), Collections.emptyList());
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(token, entity.getName(), entity.getEmail(), Instant.now().plusMillis(86_400_000));
    }

    public static class EmailAlreadyInUseException extends RuntimeException {
        public EmailAlreadyInUseException(String email) {
            super("Email already in use: " + email);
        }
    }
}
