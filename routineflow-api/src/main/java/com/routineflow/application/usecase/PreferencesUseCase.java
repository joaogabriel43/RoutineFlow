package com.routineflow.application.usecase;

import com.routineflow.application.dto.PreferencesResponse;
import com.routineflow.application.dto.UpdatePreferencesRequest;
import com.routineflow.infrastructure.persistence.entity.UserJpaEntity;
import com.routineflow.infrastructure.persistence.entity.UserPreferencesJpaEntity;
import com.routineflow.infrastructure.persistence.repository.UserJpaRepository;
import com.routineflow.infrastructure.persistence.repository.UserPreferencesJpaRepository;
import com.routineflow.infrastructure.security.AuthenticatedUserResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;

@Service
public class PreferencesUseCase {

    private final UserPreferencesJpaRepository preferencesRepository;
    private final UserJpaRepository userRepository;
    private final AuthenticatedUserResolver userResolver;

    public PreferencesUseCase(UserPreferencesJpaRepository preferencesRepository,
                              UserJpaRepository userRepository,
                              AuthenticatedUserResolver userResolver) {
        this.preferencesRepository = preferencesRepository;
        this.userRepository = userRepository;
        this.userResolver = userResolver;
    }

    @Transactional(readOnly = true)
    public PreferencesResponse getPreferences() {
        Long userId = userResolver.currentUserId();
        UserPreferencesJpaEntity entity = preferencesRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));

        return new PreferencesResponse(
                entity.getTheme(),
                entity.isSoundEnabled(),
                DayOfWeek.valueOf(entity.getFirstDayOfWeek())
        );
    }

    @Transactional
    public PreferencesResponse updatePreferences(UpdatePreferencesRequest request) {
        Long userId = userResolver.currentUserId();
        UserPreferencesJpaEntity entity = preferencesRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));

        entity.setTheme(request.theme());
        entity.setSoundEnabled(request.soundEnabled());
        entity.setFirstDayOfWeek(request.firstDayOfWeek().name());

        preferencesRepository.save(entity);

        return new PreferencesResponse(
                entity.getTheme(),
                entity.isSoundEnabled(),
                DayOfWeek.valueOf(entity.getFirstDayOfWeek())
        );
    }

    private UserPreferencesJpaEntity createDefaultPreferences(Long userId) {
        UserJpaEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        UserPreferencesJpaEntity prefs = new UserPreferencesJpaEntity(user);
        return preferencesRepository.save(prefs);
    }
}
