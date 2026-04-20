package com.routineflow.presentation.controller;

import com.routineflow.application.dto.DayScheduleResponse;
import com.routineflow.application.dto.ImportRoutineResponse;
import com.routineflow.application.dto.RoutineResponse;
import com.routineflow.application.usecase.GetActiveRoutineUseCase;
import com.routineflow.application.usecase.GetDayScheduleUseCase;
import com.routineflow.application.usecase.ImportRoutineUseCase;
import com.routineflow.infrastructure.security.AuthenticatedUserResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;

@RestController
@RequestMapping("/routines")
public class RoutineController {

    private final ImportRoutineUseCase importRoutineUseCase;
    private final GetActiveRoutineUseCase getActiveRoutineUseCase;
    private final GetDayScheduleUseCase getDayScheduleUseCase;
    private final AuthenticatedUserResolver userResolver;

    public RoutineController(
            ImportRoutineUseCase importRoutineUseCase,
            GetActiveRoutineUseCase getActiveRoutineUseCase,
            GetDayScheduleUseCase getDayScheduleUseCase,
            AuthenticatedUserResolver userResolver
    ) {
        this.importRoutineUseCase = importRoutineUseCase;
        this.getActiveRoutineUseCase = getActiveRoutineUseCase;
        this.getDayScheduleUseCase = getDayScheduleUseCase;
        this.userResolver = userResolver;
    }

    @PostMapping(value = "/import", consumes = "multipart/form-data")
    public ResponseEntity<ImportRoutineResponse> importRoutine(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        String extension = extractExtension(file.getOriginalFilename());
        Long userId = userResolver.currentUserId();
        var response = importRoutineUseCase.execute(userId, file.getInputStream(), extension);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/active")
    public ResponseEntity<RoutineResponse> getActiveRoutine() {
        Long userId = userResolver.currentUserId();
        return ResponseEntity.ok(getActiveRoutineUseCase.execute(userId));
    }

    @GetMapping("/active/today")
    public ResponseEntity<DayScheduleResponse> getTodaySchedule() {
        Long userId = userResolver.currentUserId();
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        return ResponseEntity.ok(getDayScheduleUseCase.execute(userId, today));
    }

    @GetMapping("/active/day/{dayOfWeek}")
    public ResponseEntity<DayScheduleResponse> getDaySchedule(
            @PathVariable DayOfWeek dayOfWeek
    ) {
        Long userId = userResolver.currentUserId();
        return ResponseEntity.ok(getDayScheduleUseCase.execute(userId, dayOfWeek));
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("File must have an extension");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
