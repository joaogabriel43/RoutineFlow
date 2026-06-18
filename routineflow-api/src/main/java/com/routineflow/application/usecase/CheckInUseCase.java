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
import com.routineflow.infrastructure.config.AppTimeZone;

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
    public DailyLogResponse completeTask(Long userId, Long taskId, LocalDate date, String notes) {
        if (date.isAfter(LocalDate.now(AppTimeZone.ZONE))) {
            throw new IllegalArgumentException("Cannot check in or out for future dates");
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
        if (notes != null) {
            log.setNotes(notes);
        }
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

    @Transactional
    public DailyLogResponse updateNotes(Long userId, Long taskId, LocalDate date, String notes) {
        var log = dailyLogJpaRepository
                .findByTaskIdAndUserIdAndLogDate(taskId, userId, date)
                .orElseThrow(() -> new ResourceNotFoundException("Check-in not found for task: " + taskId));

        validateOwnership(userId, log.getTask().getArea().getUser().getId(), taskId);

        log.setNotes(notes);
        log = dailyLogJpaRepository.save(log);

        return toResponse(log);
    }

    @Transactional(readOnly = true)
    public DailyLogResponse getNotes(Long userId, Long taskId, LocalDate date) {
        var log = dailyLogJpaRepository
                .findByTaskIdAndUserIdAndLogDate(taskId, userId, date)
                .orElseThrow(() -> new ResourceNotFoundException("Check-in not found for task: " + taskId));

        validateOwnership(userId, log.getTask().getArea().getUser().getId(), taskId);

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
                log.getLogDate(),
                log.getNotes()
        );
    }
}
