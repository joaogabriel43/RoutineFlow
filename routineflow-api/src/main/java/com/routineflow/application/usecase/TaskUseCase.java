package com.routineflow.application.usecase;

import com.routineflow.application.dto.CreateTaskRequest;
import com.routineflow.application.dto.ReorderTasksRequest;
import com.routineflow.application.dto.TaskResponse;
import com.routineflow.application.dto.UpdateTaskRequest;
import com.routineflow.application.usecase.exception.ResourceNotFoundException;
import com.routineflow.application.usecase.exception.UnauthorizedException;
import com.routineflow.infrastructure.persistence.entity.TaskJpaEntity;
import com.routineflow.infrastructure.persistence.repository.AreaJpaRepository;
import com.routineflow.infrastructure.persistence.repository.TaskJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskUseCase {

    private final TaskJpaRepository taskJpaRepository;
    private final AreaJpaRepository areaJpaRepository;

    public TaskUseCase(TaskJpaRepository taskJpaRepository, AreaJpaRepository areaJpaRepository) {
        this.taskJpaRepository = taskJpaRepository;
        this.areaJpaRepository = areaJpaRepository;
    }

    @Transactional
    public TaskResponse createTask(Long userId, Long areaId, CreateTaskRequest request) {
        var area = areaJpaRepository.findByIdAndUserId(areaId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Area not found: " + areaId));

        int nextOrder = taskJpaRepository.findByAreaIdOrderByOrderIndex(areaId).size();

        var task = TaskJpaEntity.builder()
                .area(area)
                .title(request.title())
                .description(request.description())
                .estimatedMinutes(request.estimatedMinutes())
                .dayOfWeek(request.dayOfWeek())
                .orderIndex(nextOrder)
                .build();

        return toResponse(taskJpaRepository.save(task));
    }

    @Transactional
    public TaskResponse updateTask(Long userId, Long taskId, UpdateTaskRequest request) {
        var task = taskJpaRepository.findByIdAndArea_User_Id(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setEstimatedMinutes(request.estimatedMinutes());
        task.setDayOfWeek(request.dayOfWeek());

        return toResponse(taskJpaRepository.save(task));
    }

    @Transactional
    public void deleteTask(Long userId, Long taskId) {
        var task = taskJpaRepository.findByIdAndArea_User_Id(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

        // DB cascade (tasks ON DELETE CASCADE → daily_logs) handles DailyLog cleanup.
        // Never delete DailyLog explicitly — it is the immutable audit trail.
        taskJpaRepository.deleteById(task.getId());
    }

    @Transactional
    public List<TaskResponse> reorderTasks(Long userId, Long areaId, ReorderTasksRequest request) {
        var tasks = taskJpaRepository.findAllById(request.taskIds());

        // Validate all tasks belong to the requesting user
        for (var task : tasks) {
            if (!task.getArea().getUser().getId().equals(userId)) {
                throw new UnauthorizedException(
                        "User " + userId + " does not own task " + task.getId());
            }
        }

        var taskById = tasks.stream()
                .collect(Collectors.toMap(TaskJpaEntity::getId, t -> t));

        var toSave = new ArrayList<TaskJpaEntity>();
        for (int i = 0; i < request.taskIds().size(); i++) {
            var task = taskById.get(request.taskIds().get(i));
            if (task != null) {
                task.setOrderIndex(i);
                toSave.add(task);
            }
        }

        taskJpaRepository.saveAll(toSave);

        return taskJpaRepository.findByAreaIdOrderByOrderIndex(areaId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── mapping ──────────────────────────────────────────────────────────────

    private TaskResponse toResponse(TaskJpaEntity task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getEstimatedMinutes(),
                task.getOrderIndex(),
                task.getDayOfWeek()
        );
    }
}
