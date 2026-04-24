package com.routineflow.presentation.controller;

import com.routineflow.application.dto.CreateSingleTaskRequest;
import com.routineflow.application.dto.SingleTaskResponse;
import com.routineflow.application.usecase.SingleTaskUseCase;
import com.routineflow.infrastructure.security.AuthenticatedUserResolver;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/single-tasks")
public class SingleTaskController {

    private final SingleTaskUseCase singleTaskUseCase;
    private final AuthenticatedUserResolver userResolver;

    public SingleTaskController(
            SingleTaskUseCase singleTaskUseCase,
            AuthenticatedUserResolver userResolver
    ) {
        this.singleTaskUseCase = singleTaskUseCase;
        this.userResolver = userResolver;
    }

    @PostMapping
    public ResponseEntity<SingleTaskResponse> create(
            @Valid @RequestBody CreateSingleTaskRequest request
    ) {
        Long userId = userResolver.currentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(singleTaskUseCase.createSingleTask(userId, request));
    }

    @GetMapping
    public ResponseEntity<List<SingleTaskResponse>> listPending() {
        Long userId = userResolver.currentUserId();
        return ResponseEntity.ok(singleTaskUseCase.listPendingTasks(userId));
    }

    @GetMapping("/archived")
    public ResponseEntity<List<SingleTaskResponse>> listArchived() {
        Long userId = userResolver.currentUserId();
        return ResponseEntity.ok(singleTaskUseCase.listArchivedTasks(userId));
    }

    /**
     * Today view: all pending tasks (regardless of dueDate — overdue ones still appear).
     * isOverdue flag indicates tasks past their due date.
     */
    @GetMapping("/today")
    public ResponseEntity<List<SingleTaskResponse>> listToday() {
        Long userId = userResolver.currentUserId();
        return ResponseEntity.ok(singleTaskUseCase.listPendingTasks(userId));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<SingleTaskResponse> complete(@PathVariable Long id) {
        Long userId = userResolver.currentUserId();
        return ResponseEntity.ok(singleTaskUseCase.completeSingleTask(userId, id));
    }

    @PostMapping("/{id}/uncomplete")
    public ResponseEntity<SingleTaskResponse> uncomplete(@PathVariable Long id) {
        Long userId = userResolver.currentUserId();
        return ResponseEntity.ok(singleTaskUseCase.uncompleteSingleTask(userId, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Long userId = userResolver.currentUserId();
        singleTaskUseCase.deleteSingleTask(userId, id);
        return ResponseEntity.noContent().build();
    }
}
