package com.routineflow.unit.usecase;

import com.routineflow.application.dto.CreateAreaRequest;
import com.routineflow.application.dto.ReorderAreasRequest;
import com.routineflow.application.dto.UpdateAreaRequest;
import com.routineflow.application.usecase.AreaUseCase;
import com.routineflow.application.usecase.exception.ResourceNotFoundException;
import com.routineflow.application.usecase.exception.UnauthorizedException;
import com.routineflow.domain.model.ResetFrequency;
import com.routineflow.infrastructure.persistence.entity.AreaJpaEntity;
import com.routineflow.infrastructure.persistence.entity.RoutineJpaEntity;
import com.routineflow.infrastructure.persistence.entity.UserJpaEntity;
import com.routineflow.infrastructure.persistence.repository.AreaJpaRepository;
import com.routineflow.infrastructure.persistence.repository.RoutineJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AreaUseCaseTest {

    @Mock private AreaJpaRepository areaJpaRepository;
    @Mock private RoutineJpaRepository routineJpaRepository;

    private AreaUseCase useCase;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long ROUTINE_ID = 10L;
    private static final Long AREA_ID = 100L;

    @BeforeEach
    void setUp() {
        useCase = new AreaUseCase(areaJpaRepository, routineJpaRepository);
    }

    // ── createArea ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("createArea_validRequest_savesAndReturnsAreaResponse")
    void createArea_validRequest_savesAndReturnsAreaResponse() {
        var request = new CreateAreaRequest("Inglês", "#3B82F6", "📚", null);
        var routine = buildRoutine(ROUTINE_ID, USER_ID);
        var savedArea = buildArea(AREA_ID, USER_ID, routine, "Inglês", "#3B82F6", "📚", 0);

        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(routine));
        when(areaJpaRepository.findByRoutineId(ROUTINE_ID)).thenReturn(List.of());
        when(areaJpaRepository.save(any())).thenReturn(savedArea);

        var result = useCase.createArea(USER_ID, request);

        assertThat(result.id()).isEqualTo(AREA_ID);
        assertThat(result.name()).isEqualTo("Inglês");
        assertThat(result.color()).isEqualTo("#3B82F6");
        assertThat(result.icon()).isEqualTo("📚");
        assertThat(result.orderIndex()).isZero();
        verify(areaJpaRepository).save(any());
    }

    @Test
    @DisplayName("createArea_noActiveRoutine_throwsResourceNotFoundException")
    void createArea_noActiveRoutine_throwsResourceNotFoundException() {
        var request = new CreateAreaRequest("Inglês", "#3B82F6", "📚", null);
        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.createArea(USER_ID, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No active routine");

        verify(areaJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("createArea_withExistingAreas_setsOrderIndexAsNextInSequence")
    void createArea_withExistingAreas_setsOrderIndexAsNextInSequence() {
        var request = new CreateAreaRequest("Nova", "#FFFFFF", "🆕", null);
        var routine = buildRoutine(ROUTINE_ID, USER_ID);
        var existing1 = buildArea(1L, USER_ID, routine, "A", "#000000", "🅰", 0);
        var existing2 = buildArea(2L, USER_ID, routine, "B", "#111111", "🅱", 1);

        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(routine));
        when(areaJpaRepository.findByRoutineId(ROUTINE_ID)).thenReturn(List.of(existing1, existing2));
        when(areaJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.createArea(USER_ID, request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<AreaJpaEntity> captor = ArgumentCaptor.forClass(AreaJpaEntity.class);
        verify(areaJpaRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderIndex()).isEqualTo(2);
    }

    // ── updateArea ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateArea_validRequest_updatesAndReturnsResponse")
    void updateArea_validRequest_updatesAndReturnsResponse() {
        var request = new UpdateAreaRequest("Inglês Avançado", "#60A5FA", "📖", null);
        var routine = buildRoutine(ROUTINE_ID, USER_ID);
        var area = buildArea(AREA_ID, USER_ID, routine, "Inglês", "#3B82F6", "📚", 0);

        when(areaJpaRepository.findByIdAndUserId(AREA_ID, USER_ID)).thenReturn(Optional.of(area));
        when(areaJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.updateArea(USER_ID, AREA_ID, request);

        assertThat(result.name()).isEqualTo("Inglês Avançado");
        assertThat(result.color()).isEqualTo("#60A5FA");
        assertThat(result.icon()).isEqualTo("📖");
        verify(areaJpaRepository).save(any());
    }

    @Test
    @DisplayName("updateArea_areaNotFound_throwsResourceNotFoundException")
    void updateArea_areaNotFound_throwsResourceNotFoundException() {
        var request = new UpdateAreaRequest("X", "#000000", "❓", null);
        when(areaJpaRepository.findByIdAndUserId(AREA_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.updateArea(USER_ID, AREA_ID, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(areaJpaRepository, never()).save(any());
    }

    // ── deleteArea ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteArea_validRequest_callsDeleteById")
    void deleteArea_validRequest_callsDeleteById() {
        var routine = buildRoutine(ROUTINE_ID, USER_ID);
        var area = buildArea(AREA_ID, USER_ID, routine, "Inglês", "#3B82F6", "📚", 0);

        when(areaJpaRepository.findByIdAndUserId(AREA_ID, USER_ID)).thenReturn(Optional.of(area));

        useCase.deleteArea(USER_ID, AREA_ID);

        verify(areaJpaRepository).deleteById(AREA_ID);
    }

    @Test
    @DisplayName("deleteArea_areaNotFound_throwsResourceNotFoundException")
    void deleteArea_areaNotFound_throwsResourceNotFoundException() {
        when(areaJpaRepository.findByIdAndUserId(AREA_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.deleteArea(USER_ID, AREA_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(areaJpaRepository, never()).deleteById(any());
    }

    // ── reorderAreas ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("reorderAreas_validRequest_savesAllWithUpdatedOrderIndex")
    void reorderAreas_validRequest_savesAllWithUpdatedOrderIndex() {
        var routine = buildRoutine(ROUTINE_ID, USER_ID);
        var area1 = buildArea(1L, USER_ID, routine, "A", "#000000", "🅰", 1);
        var area2 = buildArea(2L, USER_ID, routine, "B", "#111111", "🅱", 0);
        var request = new ReorderAreasRequest(List.of(2L, 1L)); // B first, then A

        when(areaJpaRepository.findAllById(List.of(2L, 1L))).thenReturn(List.of(area2, area1));
        when(areaJpaRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(routine));
        when(areaJpaRepository.findAreasWithTasksByRoutineIdOrderByOrderIndex(ROUTINE_ID))
                .thenReturn(List.of(area2, area1));

        useCase.reorderAreas(USER_ID, request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AreaJpaEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(areaJpaRepository).saveAll(captor.capture());
        var saved = captor.getValue();
        // area2 (id=2) should now be at index 0, area1 (id=1) at index 1
        assertThat(saved.stream().filter(a -> a.getId().equals(2L)).findFirst())
                .get().extracting(AreaJpaEntity::getOrderIndex).isEqualTo(0);
        assertThat(saved.stream().filter(a -> a.getId().equals(1L)).findFirst())
                .get().extracting(AreaJpaEntity::getOrderIndex).isEqualTo(1);
    }

    @Test
    @DisplayName("reorderAreas_areaDoesNotBelongToUser_throwsUnauthorizedException")
    void reorderAreas_areaDoesNotBelongToUser_throwsUnauthorizedException() {
        var routine = buildRoutine(ROUTINE_ID, USER_ID);
        // Area belongs to OTHER_USER_ID
        var foreignArea = buildArea(1L, OTHER_USER_ID, routine, "Foreign", "#FF0000", "🚫", 0);
        var request = new ReorderAreasRequest(List.of(1L));

        when(areaJpaRepository.findAllById(List.of(1L))).thenReturn(List.of(foreignArea));

        assertThatThrownBy(() -> useCase.reorderAreas(USER_ID, request))
                .isInstanceOf(UnauthorizedException.class);

        verify(areaJpaRepository, never()).saveAll(any());
    }

    // ── resetFrequency ───────────────────────────────────────────────────────

    @Test
    @DisplayName("createArea_withResetFrequencyWeekly_persistsWeeklyAndReturnsIt")
    void createArea_withResetFrequencyWeekly_persistsWeeklyAndReturnsIt() {
        var request = new CreateAreaRequest("Weekly Area", "#3B82F6", "📅", ResetFrequency.WEEKLY);
        var routine = buildRoutine(ROUTINE_ID, USER_ID);

        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(routine));
        when(areaJpaRepository.findByRoutineId(ROUTINE_ID)).thenReturn(List.of());
        when(areaJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.createArea(USER_ID, request);

        assertThat(result.resetFrequency()).isEqualTo(ResetFrequency.WEEKLY);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<AreaJpaEntity> captor = ArgumentCaptor.forClass(AreaJpaEntity.class);
        verify(areaJpaRepository).save(captor.capture());
        assertThat(captor.getValue().getResetFrequency()).isEqualTo(ResetFrequency.WEEKLY);
    }

    @Test
    @DisplayName("createArea_nullResetFrequency_defaultsToDaily")
    void createArea_nullResetFrequency_defaultsToDaily() {
        var request = new CreateAreaRequest("Daily Area", "#3B82F6", "📅", null);
        var routine = buildRoutine(ROUTINE_ID, USER_ID);

        when(routineJpaRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(routine));
        when(areaJpaRepository.findByRoutineId(ROUTINE_ID)).thenReturn(List.of());
        when(areaJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.createArea(USER_ID, request);

        assertThat(result.resetFrequency()).isEqualTo(ResetFrequency.DAILY);
    }

    @Test
    @DisplayName("updateArea_withNewResetFrequency_updatesAndReturnsNewFrequency")
    void updateArea_withNewResetFrequency_updatesAndReturnsNewFrequency() {
        var request = new UpdateAreaRequest("Inglês", "#3B82F6", "📚", ResetFrequency.MONTHLY);
        var routine = buildRoutine(ROUTINE_ID, USER_ID);
        var area = buildArea(AREA_ID, USER_ID, routine, "Inglês", "#3B82F6", "📚", 0);

        when(areaJpaRepository.findByIdAndUserId(AREA_ID, USER_ID)).thenReturn(Optional.of(area));
        when(areaJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.updateArea(USER_ID, AREA_ID, request);

        assertThat(result.resetFrequency()).isEqualTo(ResetFrequency.MONTHLY);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private RoutineJpaEntity buildRoutine(Long routineId, Long userId) {
        var user = UserJpaEntity.builder().id(userId).build();
        return RoutineJpaEntity.builder()
                .id(routineId)
                .user(user)
                .name("Test Routine")
                .active(true)
                .build();
    }

    private AreaJpaEntity buildArea(Long id, Long userId, RoutineJpaEntity routine,
                                    String name, String color, String icon, int orderIndex) {
        var user = UserJpaEntity.builder().id(userId).build();
        return AreaJpaEntity.builder()
                .id(id)
                .user(user)
                .routine(routine)
                .name(name)
                .color(color)
                .icon(icon)
                .orderIndex(orderIndex)
                .tasks(new java.util.ArrayList<>())
                .build();
    }
}
