package com.routineflow.presentation.controller;

import com.routineflow.application.dto.AuthResponse;
import com.routineflow.application.dto.ChangePasswordRequest;
import com.routineflow.application.dto.LoginRequest;
import com.routineflow.application.dto.ProfileResponse;
import com.routineflow.application.dto.RegisterRequest;
import com.routineflow.application.dto.UpdateProfileRequest;
import com.routineflow.application.usecase.ChangePasswordUseCase;
import com.routineflow.application.usecase.LoginUseCase;
import com.routineflow.application.usecase.RegisterUserUseCase;
import com.routineflow.application.usecase.UpdateProfileUseCase;
import com.routineflow.application.usecase.RevokeSessionsUseCase;
import com.routineflow.infrastructure.security.AuthenticatedUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "User registration, authentication, and profile management")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final RevokeSessionsUseCase revokeSessionsUseCase;
    private final AuthenticatedUserResolver userResolver;

    public AuthController(
            RegisterUserUseCase registerUserUseCase,
            LoginUseCase loginUseCase,
            UpdateProfileUseCase updateProfileUseCase,
            ChangePasswordUseCase changePasswordUseCase,
            RevokeSessionsUseCase revokeSessionsUseCase,
            AuthenticatedUserResolver userResolver
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUseCase = loginUseCase;
        this.updateProfileUseCase = updateProfileUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
        this.revokeSessionsUseCase = revokeSessionsUseCase;
        this.userResolver = userResolver;
    }

    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = registerUserUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Login and receive a JWT token (rate-limited: 10 req/min per IP)")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = loginUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get the authenticated user's profile")
    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile() {
        Long userId = userResolver.currentUserId();
        ProfileResponse response = updateProfileUseCase.getProfile(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update the authenticated user's name")
    @PutMapping("/profile")
    public ResponseEntity<ProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = userResolver.currentUserId();
        ProfileResponse response = updateProfileUseCase.execute(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Change the authenticated user's password")
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = userResolver.currentUserId();
        changePasswordUseCase.execute(userId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Revoke all active sessions for the user")
    @PostMapping("/revoke-sessions")
    public ResponseEntity<Void> revokeSessions() {
        revokeSessionsUseCase.execute();
        return ResponseEntity.noContent().build();
    }
}
