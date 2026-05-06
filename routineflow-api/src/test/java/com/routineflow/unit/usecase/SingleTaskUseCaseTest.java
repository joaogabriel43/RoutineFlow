package com.routineflow.unit.usecase;

import com.routineflow.application.dto.CreateSingleTaskRequest;
import com.routineflow.application.usecase.SingleTaskUseCase;
import com.routineflow.application.usecase.exception.UnauthorizedException;
import com.routineflow.infrastructure.persistence.entity.SingleTaskJpaEntity;
import com.routineflow.infrastructure.persistence.repository.SingleTaskJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SingleTaskUseCaseTest {

    @Mock
    private SingleTaskJpaRepository singleTaskJpaRepository;

    private SingleTaskUseCase useCase;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long TASK_ID = 10L;

    @BeforeEach
    void setUp() {
        useCase = new SingleTaskUseCase(singleTaskJpaRepository);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createSingleTask_validTitle_persistsAndReturnsSingleTaskResponse")
    void createSingleTask_validTitle_persistsAndReturnsSingleTaskResponse() {
        var request = new CreateSingleTaskRequest("Comprar leite", null, null);
        when(singleTaskJpaRepository.save(any())).thenAnswer(inv -> {
            var e = (SingleTaskJpaEntity) inv.getArgument(0);
            e.setId(TASK_ID);
            return e;
        });

        var result = useCase.createSingleTask(USER_ID, request);

        assertThat(result.title()).isEqualTo("Comprar leite");
        assertThat(result.completed()).isFalse();
        assertThat(result.isOverdue()).isFalse();
        verify(singleTaskJpaRepository).save(any());
    }

    @Test
    @DisplayName("createSingleTask_emptyTitle_throwsIllegalArgumentException")
    void createSingleTask_emptyTitle_throwsIllegalArgumentException() {
        var request = new CreateSingleTaskRequest("   ", null, null);

        assertThatThrownBy(() -> useCase.createSingleTask(USER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");

        verify(singleTaskJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("createSingleTask_pastDueDate_throwsIllegalArgumentException")
    void createSingleTask_pastDueDate_throwsIllegalArgumentException() {
        var yesterday = LocalDate.now().minusDays(1);
        var request = new CreateSingleTaskRequest("Reunião", null, yesterday);

        assertThatThrownBy(() -> useCase.createSingleTask(USER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("due date");

        verify(singleTaskJpaRepository, never()).save(any());
    }

    // ── complete ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("completeSingleTask_pendingTask_setsCompletedAndArchivedAt")
    void completeSingleTask_pendingTask_setsCompletedAndArchivedAt() {
        var entity = pendingTask(TASK_ID, USER_ID);
        when(singleTaskJpaRepository.findById(TASK_ID)).thenReturn(Optional.of(entity));
        when(singleTaskJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.completeSingleTask(USER_ID, TASK_ID);

        assertThat(result.completed()).isTrue();
        assertThat(result.completedAt()).isNotNull();
        verify(singleTaskJpaRepository).save(entity);
    }

    @Test
    @DisplayName("completeSingleTask_alreadyComplete_throwsIllegalStateException")
    void completeSingleTask_alreadyComplete_throwsIllegalStateException() {
        var entity = completedTask(TASK_ID, USER_ID);
        when(singleTaskJpaRepository.findById(TASK_ID)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> useCase.completeSingleTask(USER_ID, TASK_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already completed");

        verify(singleTaskJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("completeSingleTask_otherUserTask_throwsUnauthorizedException")
    void completeSingleTask_otherUserTask_throwsUnauthorizedException() {
        var entity = pendingTask(TASK_ID, OTHER_USER_ID);
        when(singleTaskJpaRepository.findById(TASK_ID)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> useCase.completeSingleTask(USER_ID, TASK_ID))
                .isInstanceOf(UnauthorizedException.class);

        verify(singleTaskJpaRepository, never()).save(any());
    }

    // ── uncomplete ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("uncompleteSingleTask_completedTask_revertsToP ending")
    void uncompleteSingleTask_completedTask_revertsToPending() {
        var entity = completedTask(TASK_ID, USER_ID);
        when(singleTaskJpaRepository.findById(TASK_ID)).thenReturn(Optional.of(entity));
        when(singleTaskJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.uncompleteSingleTask(USER_ID, TASK_ID);

        assertThat(result.completed()).isFalse();
        assertThat(result.completedAt()).isNull();
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteSingleTask_ownedTask_deletesFromRepository")
    void deleteSingleTask_ownedTask_deletesFromRepository() {
        var entity = pendingTask(TASK_ID, USER_ID);
        when(singleTaskJpaRepository.findById(TASK_ID)).thenReturn(Optional.of(entity));

        useCase.deleteSingleTask(USER_ID, TASK_ID);

        verify(singleTaskJpaRepository).deleteById(TASK_ID);
    }

    // ── list pending ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("listPendingTasks_returnsOnlyNonCompletedTasks")
    void listPendingTasks_returnsOnlyNonCompletedTasks() {
        var t1 = pendingTask(1L, USER_ID);
        t1.setTitle("Task A");
        var t2 = pendingTask(2L, USER_ID);
        t2.setTitle("Task B");

        when(singleTaskJpaRepository.findPendingByUserId(USER_ID)).thenReturn(List.of(t1, t2));

        var result = useCase.listPendingTasks(USER_ID);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> !r.completed());
    }

    // ── list archived ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("listArchivedTasks_returnsOnlyCompletedTasks")
    void listArchivedTasks_returnsOnlyCompletedTasks() {
        var t1 = completedTask(1L, USER_ID);
        t1.setTitle("Done A");

        when(singleTaskJpaRepository.findArchivedByUserId(USER_ID)).thenReturn(List.of(t1));

        var result = useCase.listArchivedTasks(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).completed()).isTrue();
        assertThat(result.get(0).completedAt()).isNotNull();
    }

    // ── builders ──────────────────────────────────────────────────────────────

    private SingleTaskJpaEntity pendingTask(Long id, Long userId) {
        return SingleTaskJpaEntity.builder()
                .id(id)
                .userId(userId)
                .title("Task " + id)
                .completed(false)
                .createdAt(Instant.now())
                .build();
    }

    private SingleTaskJpaEntity completedTask(Long id, Long userId) {
        return SingleTaskJpaEntity.builder()
                .id(id)
                .userId(userId)
                .title("Task " + id)
                .completed(true)
                .completedAt(Instant.now())
                .archivedAt(Instant.now())
                .createdAt(Instant.now())
                .build();
    }
}
