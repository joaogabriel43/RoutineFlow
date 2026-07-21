package com.routineflow.unit.application;

import com.routineflow.application.dto.PreferencesResponse;
import com.routineflow.application.dto.UpdatePreferencesRequest;
import com.routineflow.application.usecase.PreferencesUseCase;
import com.routineflow.infrastructure.persistence.entity.UserJpaEntity;
import com.routineflow.infrastructure.persistence.entity.UserPreferencesJpaEntity;
import com.routineflow.infrastructure.persistence.repository.UserJpaRepository;
import com.routineflow.infrastructure.persistence.repository.UserPreferencesJpaRepository;
import com.routineflow.infrastructure.security.AuthenticatedUserResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreferencesUseCaseTest {

    @Mock
    private UserPreferencesJpaRepository preferencesRepository;

    @Mock
    private UserJpaRepository userRepository;

    @Mock
    private AuthenticatedUserResolver userResolver;

    @InjectMocks
    private PreferencesUseCase useCase;

    private UserJpaEntity testUser;
    private UserPreferencesJpaEntity testPrefs;

    @BeforeEach
    void setUp() {
        testUser = new UserJpaEntity();
        testUser.setId(1L);

        testPrefs = new UserPreferencesJpaEntity(testUser);
        testPrefs.setTheme("SYSTEM");
        testPrefs.setSoundEnabled(false);
        testPrefs.setFirstDayOfWeek("MONDAY");
    }

    @Test
    void getPreferences_existing_returnsDto() {
        when(userResolver.currentUserId()).thenReturn(1L);
        when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPrefs));

        PreferencesResponse response = useCase.getPreferences();

        assertNotNull(response);
        assertEquals("SYSTEM", response.theme());
        assertFalse(response.soundEnabled());
        assertEquals(DayOfWeek.MONDAY, response.firstDayOfWeek());
    }

    @Test
    void getPreferences_notExisting_createsDefaultAndReturns() {
        when(userResolver.currentUserId()).thenReturn(1L);
        when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(preferencesRepository.save(any())).thenReturn(testPrefs);

        PreferencesResponse response = useCase.getPreferences();

        assertNotNull(response);
        assertEquals("SYSTEM", response.theme());
        verify(preferencesRepository).save(any(UserPreferencesJpaEntity.class));
    }

    @Test
    void updatePreferences_success_updatesAndReturns() {
        when(userResolver.currentUserId()).thenReturn(1L);
        when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(testPrefs));

        UpdatePreferencesRequest request = new UpdatePreferencesRequest("DARK", true, DayOfWeek.SUNDAY);
        PreferencesResponse response = useCase.updatePreferences(request);

        assertNotNull(response);
        assertEquals("DARK", response.theme());
        assertTrue(response.soundEnabled());
        assertEquals(DayOfWeek.SUNDAY, response.firstDayOfWeek());

        verify(preferencesRepository).save(testPrefs);
    }
}
