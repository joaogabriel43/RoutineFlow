package com.routineflow.unit.usecase;

import com.routineflow.application.dto.ChangePasswordRequest;
import com.routineflow.application.usecase.ChangePasswordUseCase;
import com.routineflow.infrastructure.persistence.entity.UserJpaEntity;
import com.routineflow.infrastructure.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangePasswordUseCaseTest {

    @Mock
    private UserJpaRepository userJpaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ChangePasswordUseCase useCase;

    @Test
    void execute_validCurrentPassword_changesPassword() {
        var user = UserJpaEntity.builder()
                .id(1L).name("User").email("u@t.com").passwordHash("$encoded$old").build();
        when(userJpaRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-pass", "$encoded$old")).thenReturn(true);
        when(passwordEncoder.encode("new-pass")).thenReturn("$encoded$new");

        assertThatCode(() -> useCase.execute(1L, new ChangePasswordRequest("old-pass", "new-pass")))
                .doesNotThrowAnyException();

        verify(userJpaRepository).save(user);
        verify(passwordEncoder).encode("new-pass");
    }

    @Test
    void execute_wrongCurrentPassword_throwsIllegalArgument() {
        var user = UserJpaEntity.builder()
                .id(1L).name("User").email("u@t.com").passwordHash("$encoded$old").build();
        when(userJpaRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "$encoded$old")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(1L, new ChangePasswordRequest("wrong", "new-pass")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Current password is incorrect");

        verify(userJpaRepository, never()).save(any());
    }

    @Test
    void execute_samePassword_throwsIllegalArgument() {
        var user = UserJpaEntity.builder()
                .id(1L).name("User").email("u@t.com").passwordHash("$encoded$same").build();
        when(userJpaRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("same-pass", "$encoded$same")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(1L, new ChangePasswordRequest("same-pass", "same-pass")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different");

        verify(userJpaRepository, never()).save(any());
    }

    @Test
    void execute_userNotFound_throwsIllegalArgument() {
        when(userJpaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(99L, new ChangePasswordRequest("any", "any")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }
}
