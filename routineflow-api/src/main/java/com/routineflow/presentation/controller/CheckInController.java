package com.routineflow.presentation.controller;

import com.routineflow.application.dto.DailyLogResponse;
import com.routineflow.application.dto.DailyProgressResponse;
import com.routineflow.application.usecase.CheckInUseCase;
import com.routineflow.application.usecase.GetDailyProgressUseCase;
import com.routineflow.infrastructure.security.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/checkins")
public class CheckInController {

    private final CheckInUseCase checkInUseCase;
    private final GetDailyProgressUseCase getDailyProgressUseCase;
    private final AuthenticatedUserResolver userResolver;

    public CheckInController(
            CheckInUseCase checkInUseCase,
            GetDailyProgressUseCase getDailyProgressUseCase,
            AuthenticatedUserResolver userResolver
    ) {
        this.checkInUseCase = checkInUseCase;
        this.getDailyProgressUseCase = getDailyProgressUseCase;
        this.userResolver = userResolver;
    }

    @PostMapping("/{taskId}/complete")
    public ResponseEntity<DailyLogResponse> complete(@PathVariable Long taskId) {
        Long userId = userResolver.currentUserId();
        return ResponseEntity.ok(checkInUseCase.completeTask(userId, taskId, LocalDate.now()));
    }

    @PostMapping("/{taskId}/uncomplete")
    public ResponseEntity<DailyLogResponse> uncomplete(@PathVariable Long taskId) {
        Long userId = userResolver.currentUserId();
        return ResponseEntity.ok(checkInUseCase.uncompleteTask(userId, taskId, LocalDate.now()));
    }

    @GetMapping("/today/progress")
    public ResponseEntity<DailyProgressResponse> todayProgress() {
        Long userId = userResolver.currentUserId();
        return ResponseEntity.ok(getDailyProgressUseCase.getProgress(userId, LocalDate.now()));
    }
}
