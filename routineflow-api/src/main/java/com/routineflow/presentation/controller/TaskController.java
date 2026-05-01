package com.routineflow.presentation.controller;

import com.routineflow.application.dto.CreateTaskRequest;
import com.routineflow.application.dto.ReorderTasksRequest;
import com.routineflow.application.dto.TaskResponse;
import com.routineflow.application.dto.UpdateTaskRequest;
import com.routineflow.application.usecase.TaskUseCase;
import com.routineflow.infrastructure.security.AuthenticatedUserResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Tasks", description = "Task CRUD and reordering within an area")
@RestController
@RequestMapping("/areas/{areaId}/tasks")
public class TaskController {

    private final TaskUseCase taskUseCase;
    private final AuthenticatedUserResolver userResolver;

    public TaskController(TaskUseCase taskUseCase, AuthenticatedUserResolver userResolver) {
        this.taskUseCase = taskUseCase;
        this.userResolver = userResolver;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable Long areaId,
            @Valid @RequestBody CreateTaskRequest request
    ) {
        Long userId = userResolver.currentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskUseCase.createTask(userId, areaId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long areaId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        Long userId = userResolver.currentUserId();
        return ResponseEntity.ok(taskUseCase.updateTask(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long areaId,
            @PathVariable Long id
    ) {
        Long userId = userResolver.currentUserId();
        taskUseCase.deleteTask(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/reorder")
    public ResponseEntity<List<TaskResponse>> reorderTasks(
            @PathVariable Long areaId,
            @Valid @RequestBody ReorderTasksRequest request
    ) {
        Long userId = userResolver.currentUserId();
        return ResponseEntity.ok(taskUseCase.reorderTasks(userId, areaId, request));
    }
}
