package com.routineflow.presentation.controller;

import com.routineflow.application.dto.DailyLogResponse;
import com.routineflow.application.dto.DailyProgressResponse;
import com.routineflow.application.usecase.CheckInUseCase;
import com.routineflow.application.usecase.GetDailyProgressUseCase;
import com.routineflow.infrastructure.config.AppTimeZone;
import com.routineflow.infrastructure.security.AuthenticatedUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Check-ins", description = "Daily task completion and progress tracking")
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

    @Operation(summary = "Mark a task as completed for the given date (defaults to today BRT)")
    @PostMapping("/{taskId}/complete")
    public ResponseEntity<DailyLogResponse> complete(
            @PathVariable Long taskId,
            @RequestParam(required = false) LocalDate date
    ) {
        Long userId = userResolver.currentUserId();
        LocalDate targetDate = date != null ? date : LocalDate.now(AppTimeZone.ZONE);
        return ResponseEntity.ok(checkInUseCase.completeTask(userId, taskId, targetDate));
    }

    @Operation(summary = "Unmark a task (undo completion) for the given date (defaults to today BRT)")
    @PostMapping("/{taskId}/uncomplete")
    public ResponseEntity<DailyLogResponse> uncomplete(
            @PathVariable Long taskId,
            @RequestParam(required = false) LocalDate date
    ) {
        Long userId = userResolver.currentUserId();
        LocalDate targetDate = date != null ? date : LocalDate.now(AppTimeZone.ZONE);
        return ResponseEntity.ok(checkInUseCase.uncompleteTask(userId, taskId, targetDate));
    }

    // Primary progress endpoint — optional date param, defaults to today
    @Operation(summary = "Get completion progress for the given date (defaults to today BRT)")
    @GetMapping("/progress")
    public ResponseEntity<DailyProgressResponse> progress(
            @RequestParam(required = false) LocalDate date
    ) {
        Long userId = userResolver.currentUserId();
        LocalDate targetDate = date != null ? date : LocalDate.now(AppTimeZone.ZONE);
        return ResponseEntity.ok(getDailyProgressUseCase.getProgress(userId, targetDate));
    }

    // Alias for backward compatibility — keep until frontend is fully migrated
    @Operation(summary = "Get completion progress for today (alias for /progress)")
    @GetMapping("/today/progress")
    public ResponseEntity<DailyProgressResponse> todayProgress() {
        Long userId = userResolver.currentUserId();
        return ResponseEntity.ok(getDailyProgressUseCase.getProgress(userId, LocalDate.now(AppTimeZone.ZONE)));
    }
}
