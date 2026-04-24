package com.routineflow.application.usecase;

import com.routineflow.application.dto.CreateSingleTaskRequest;
import com.routineflow.application.dto.SingleTaskResponse;
import com.routineflow.application.usecase.exception.ResourceNotFoundException;
import com.routineflow.application.usecase.exception.UnauthorizedException;
import com.routineflow.infrastructure.persistence.entity.SingleTaskJpaEntity;
import com.routineflow.infrastructure.persistence.repository.SingleTaskJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class SingleTaskUseCase {

    private final SingleTaskJpaRepository singleTaskJpaRepository;

    public SingleTaskUseCase(SingleTaskJpaRepository singleTaskJpaRepository) {
        this.singleTaskJpaRepository = singleTaskJpaRepository;
    }

    @Transactional
    public SingleTaskResponse createSingleTask(Long userId, CreateSingleTaskRequest request) {
        // Use case-level validation (belt-and-suspenders — controller also validates via @Valid)
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("Single task title must not be blank");
        }
        if (request.dueDate() != null && request.dueDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Single task due date cannot be in the past");
        }

        var entity = SingleTaskJpaEntity.builder()
                .userId(userId)
                .title(request.title().strip())
                .description(request.description())
                .dueDate(request.dueDate())
                .completed(false)
                .build();

        entity = singleTaskJpaRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional
    public SingleTaskResponse completeSingleTask(Long userId, Long taskId) {
        var entity = findAndVerifyOwnership(userId, taskId);

        if (entity.isCompleted()) {
            throw new IllegalStateException("Single task " + taskId + " is already completed");
        }

        var now = Instant.now();
        entity.setCompleted(true);
        entity.setCompletedAt(now);
        entity.setArchivedAt(now);
        entity = singleTaskJpaRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional
    public SingleTaskResponse uncompleteSingleTask(Long userId, Long taskId) {
        var entity = findAndVerifyOwnership(userId, taskId);

        entity.setCompleted(false);
        entity.setCompletedAt(null);
        entity.setArchivedAt(null);
        entity = singleTaskJpaRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional
    public void deleteSingleTask(Long userId, Long taskId) {
        findAndVerifyOwnership(userId, taskId);
        singleTaskJpaRepository.deleteById(taskId);
    }

    @Transactional(readOnly = true)
    public List<SingleTaskResponse> listPendingTasks(Long userId) {
        return singleTaskJpaRepository.findPendingByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SingleTaskResponse> listArchivedTasks(Long userId) {
        return singleTaskJpaRepository.findArchivedByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private SingleTaskJpaEntity findAndVerifyOwnership(Long userId, Long taskId) {
        var entity = singleTaskJpaRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Single task not found: " + taskId));
        if (!entity.getUserId().equals(userId)) {
            throw new UnauthorizedException("User " + userId + " does not own single task " + taskId);
        }
        return entity;
    }

    private SingleTaskResponse toResponse(SingleTaskJpaEntity e) {
        boolean overdue = e.getDueDate() != null
                && e.getDueDate().isBefore(LocalDate.now())
                && !e.isCompleted();
        return new SingleTaskResponse(
                e.getId(),
                e.getTitle(),
                e.getDescription(),
                e.getDueDate(),
                e.isCompleted(),
                e.getCompletedAt(),
                e.getCreatedAt(),
                overdue
        );
    }
}
