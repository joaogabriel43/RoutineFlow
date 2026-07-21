package com.routineflow.application.usecase;

import com.routineflow.application.dto.ProfileResponse;
import com.routineflow.application.dto.UpdateProfileRequest;
import com.routineflow.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateProfileUseCase {

    private final UserJpaRepository userJpaRepository;

    public UpdateProfileUseCase(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    public ProfileResponse execute(Long userId, UpdateProfileRequest request) {
        var user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setName(request.name().trim());
        userJpaRepository.save(user);

        return new ProfileResponse(user.getName(), user.getEmail());
    }

    public ProfileResponse getProfile(Long userId) {
        var user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return new ProfileResponse(user.getName(), user.getEmail());
    }
}
