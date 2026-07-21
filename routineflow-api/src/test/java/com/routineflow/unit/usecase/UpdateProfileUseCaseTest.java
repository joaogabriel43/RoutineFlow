package com.routineflow.unit.usecase;

import com.routineflow.application.dto.ProfileResponse;
import com.routineflow.application.dto.UpdateProfileRequest;
import com.routineflow.application.usecase.UpdateProfileUseCase;
import com.routineflow.infrastructure.persistence.entity.UserJpaEntity;
import com.routineflow.infrastructure.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateProfileUseCaseTest {

    @Mock
    private UserJpaRepository userJpaRepository;

    @InjectMocks
    private UpdateProfileUseCase useCase;

    @Test
    void execute_validName_updatesAndReturnsProfile() {
        var user = UserJpaEntity.builder()
                .id(1L).name("Old Name").email("user@test.com").passwordHash("hash").build();
        when(userJpaRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userJpaRepository.save(user)).thenReturn(user);

        ProfileResponse result = useCase.execute(1L, new UpdateProfileRequest("New Name"));

        assertThat(result.name()).isEqualTo("New Name");
        assertThat(result.email()).isEqualTo("user@test.com");
        verify(userJpaRepository).save(user);
    }

    @Test
    void execute_trimmedName_trimsWhitespace() {
        var user = UserJpaEntity.builder()
                .id(1L).name("Old").email("u@t.com").passwordHash("h").build();
        when(userJpaRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userJpaRepository.save(user)).thenReturn(user);

        ProfileResponse result = useCase.execute(1L, new UpdateProfileRequest("  Trimmed  "));

        assertThat(result.name()).isEqualTo("Trimmed");
    }

    @Test
    void execute_userNotFound_throwsIllegalArgument() {
        when(userJpaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(99L, new UpdateProfileRequest("Name")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void getProfile_existingUser_returnsProfile() {
        var user = UserJpaEntity.builder()
                .id(1L).name("João").email("joao@test.com").passwordHash("hash").build();
        when(userJpaRepository.findById(1L)).thenReturn(Optional.of(user));

        ProfileResponse result = useCase.getProfile(1L);

        assertThat(result.name()).isEqualTo("João");
        assertThat(result.email()).isEqualTo("joao@test.com");
    }
}
