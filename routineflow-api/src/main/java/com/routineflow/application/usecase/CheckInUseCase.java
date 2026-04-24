package com.routineflow.application.usecase;

import com.routineflow.application.dto.DailyLogResponse;
import com.routineflow.application.usecase.exception.ResourceNotFoundException;
import com.routineflow.application.usecase.exception.UnauthorizedException;
import com.routineflow.infrastructure.persistence.entity.DailyLogJpaEntity;
import com.routineflow.infrastructure.persistence.repository.DailyLogJpaRepository;
import com.routineflow.infrastructure.persistence.repository.TaskJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

@Service
public class CheckInUseCase {

    private final TaskJpaRepository taskJpaRepository;
    private final DailyLogJpaRepository dailyLogJpaRepository;

    public CheckInUseCase(
            TaskJpaRepository taskJpaRepository,
            DailyLogJpaRepository dailyLogJpaRepository
    ) {
        this.taskJpaRepository = taskJpaRepository;
        this.dailyLogJpaRepository = dailyLogJpaRepository;
    }

    @Transactional
    public DailyLogResponse completeTask(Long userId, Long taskId, LocalDate date) {
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot check in for a future date: " + date);
        }

        var task = taskJpaRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

        validateOwnership(userId, task.getArea().getUser().getId(), taskId);

        var log = dailyLogJpaRepository
                .findByTaskIdAndUserIdAndLogDate(taskId, userId, date)
                .orElseGet(() -> DailyLogJpaEntity.builder()
                        .task(task)
                        .user(task.getArea().getUser())
                        .logDate(date)
                        .completed(false)
                        .build());

        log.setCompleted(true);
        log.setCompletedAt(Instant.now());
        log = dailyLogJpaRepository.save(log);

        return toResponse(log);
    }

    @Transactional
    public DailyLogResponse uncompleteTask(Long userId, Long taskId, LocalDate date) {
        var task = taskJpaRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

        validateOwnership(userId, task.getArea().getUser().getId(), taskId);

        var log = dailyLogJpaRepository
                .findByTaskIdAndUserIdAndLogDate(taskId, userId, date)
                .orElseGet(() -> DailyLogJpaEntity.builder()
                        .task(task)
                        .user(task.getArea().getUser())
                        .logDate(date)
                        .completed(false)
                        .build());

        log.setCompleted(false);
        log.setCompletedAt(null);
        log = dailyLogJpaRepository.save(log);

        return toResponse(log);
    }

    private void validateOwnership(Long requestingUserId, Long taskOwnerId, Long taskId) {
        if (!requestingUserId.equals(taskOwnerId)) {
            throw new UnauthorizedException(
                    "User " + requestingUserId + " does not own task " + taskId);
        }
    }

    private DailyLogResponse toResponse(DailyLogJpaEntity log) {
        return new DailyLogResponse(
                log.getTask().getId(),
                log.isCompleted(),
                log.getCompletedAt(),
                log.getLogDate()
        );
    }
}
