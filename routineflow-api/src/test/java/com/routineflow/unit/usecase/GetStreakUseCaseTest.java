package com.routineflow.unit.usecase;

import com.routineflow.application.usecase.GetStreakUseCase;
import com.routineflow.infrastructure.persistence.entity.AreaJpaEntity;
import com.routineflow.infrastructure.persistence.entity.RoutineJpaEntity;
import com.routineflow.infrastructure.persistence.entity.StreakJpaEntity;
import com.routineflow.infrastructure.persistence.entity.UserJpaEntity;
import com.routineflow.infrastructure.persistence.repository.AreaJpaRepository;
import com.routineflow.infrastructure.persistence.repository.RoutineJpaRepository;
import com.routineflow.infrastructure.persistence.repository.StreakJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStreakUseCaseTest {

    @Mock private RoutineJpaRepository routineJpaRepository;
    @Mock private AreaJpaRepository areaJpaRepository;
    @Mock private StreakJpaRepository streakJpaRepository;

    private GetStreakUseCase useCase;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        useCase = new GetStreakUseCase(routineJpaRepository, areaJpaRepository, streakJpaRepository);
    }

    @Test
    @DisplayName("getStreaks_twoAreasWithStreaks_returnsDescendingOrder")
    void getStreaks_twoAreasWithStreaks_returnsDescendingOrder() {
        var user = UserJpaEntity.builder().id(USER_ID).build();
        var routine = RoutineJpaEntity.builder().id(1L).user(user).build();

        var area1 = buildArea(1L, "Inglês", "#3B82F6", "📚", user, routine);
        var area2 = buildArea(2L, "Exercício", "#10B981", "🏋️", user, routine);

        var streak1 = StreakJpaEntity.builder()
                .id(1L).area(area1).user(user)
                .currentCount(5).lastActiveDate(LocalDate.of(2026, 4, 18)).build();
        var streak2 = StreakJpaEntity.builder()
                .id(2L).area(area2).user(user)
                .currentCount(3).lastActiveDate(LocalDate.of(2026, 4, 17)).build();

        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(routine));
        when(areaJpaRepository.findByRoutineId(1L)).thenReturn(List.of(area1, area2));
        when(streakJpaRepository.findAllByUserId(USER_ID)).thenReturn(List.of(streak1, streak2));

        var result = useCase.getStreaks(USER_ID);

        assertThat(result.streaks()).hasSize(2);
        assertThat(result.streaks().get(0).currentStreak()).isEqualTo(5);
        assertThat(result.streaks().get(0).areaName()).isEqualTo("Inglês");
        assertThat(result.streaks().get(0).color()).isEqualTo("#3B82F6");
        assertThat(result.streaks().get(0).lastActiveDate()).isEqualTo(LocalDate.of(2026, 4, 18));
        assertThat(result.streaks().get(1).currentStreak()).isEqualTo(3);
        assertThat(result.streaks().get(1).areaName()).isEqualTo("Exercício");
    }

    @Test
    @DisplayName("getStreaks_areaWithNoStreakRow_returnsZeroStreakAndNullDate")
    void getStreaks_areaWithNoStreakRow_returnsZeroStreakAndNullDate() {
        var user = UserJpaEntity.builder().id(USER_ID).build();
        var routine = RoutineJpaEntity.builder().id(1L).user(user).build();
        var area = buildArea(1L, "Leitura", "#F59E0B", "📖", user, routine);

        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(routine));
        when(areaJpaRepository.findByRoutineId(1L)).thenReturn(List.of(area));
        when(streakJpaRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

        var result = useCase.getStreaks(USER_ID);

        assertThat(result.streaks()).hasSize(1);
        var streakResponse = result.streaks().get(0);
        assertThat(streakResponse.areaId()).isEqualTo(1L);
        assertThat(streakResponse.areaName()).isEqualTo("Leitura");
        assertThat(streakResponse.currentStreak()).isEqualTo(0);
        assertThat(streakResponse.lastActiveDate()).isNull();
    }

    @Test
    @DisplayName("getStreaks_noActiveRoutine_returnsEmptyList")
    void getStreaks_noActiveRoutine_returnsEmptyList() {
        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.empty());

        var result = useCase.getStreaks(USER_ID);

        assertThat(result.streaks()).isEmpty();
    }

    private AreaJpaEntity buildArea(Long id, String name, String color, String icon,
                                    UserJpaEntity user, RoutineJpaEntity routine) {
        return AreaJpaEntity.builder()
                .id(id).name(name).color(color).icon(icon)
                .user(user).routine(routine).build();
    }
}
