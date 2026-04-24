package com.routineflow.presentation.controller;

import com.routineflow.application.dto.DayScheduleResponse;
import com.routineflow.application.dto.ImportRoutineResponse;
import com.routineflow.application.dto.RoutineResponse;
import com.routineflow.application.usecase.GetActiveRoutineUseCase;
import com.routineflow.application.usecase.GetDayScheduleUseCase;
import com.routineflow.application.usecase.ImportRoutineUseCase;
import com.routineflow.infrastructure.persistence.entity.RoutineJpaEntity;
import com.routineflow.infrastructure.persistence.repository.RoutineJpaRepository;
import com.routineflow.infrastructure.security.AuthenticatedUserResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/routines")
public class RoutineController {

    private final ImportRoutineUseCase importRoutineUseCase;
    private final GetActiveRoutineUseCase getActiveRoutineUseCase;
    private final GetDayScheduleUseCase getDayScheduleUseCase;
    private final RoutineJpaRepository routineJpaRepository;
    private final AuthenticatedUserResolver userResolver;

    public RoutineController(
            ImportRoutineUseCase importRoutineUseCase,
            GetActiveRoutineUseCase getActiveRoutineUseCase,
            GetDayScheduleUseCase getDayScheduleUseCase,
            RoutineJpaRepository routineJpaRepository,
            AuthenticatedUserResolver userResolver
    ) {
        this.importRoutineUseCase = importRoutineUseCase;
        this.getActiveRoutineUseCase = getActiveRoutineUseCase;
        this.getDayScheduleUseCase = getDayScheduleUseCase;
        this.routineJpaRepository = routineJpaRepository;
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

    // ── Temporary recovery endpoints ──────────────────────────────────────────
    // These endpoints allow recovering an accidentally deactivated routine.
    // They bypass the use-case layer intentionally — remove once no longer needed.

    @GetMapping("/all")
    public ResponseEntity<List<Map<String, Object>>> getAllRoutines() {
        Long userId = userResolver.currentUserId();
        List<Map<String, Object>> result = routineJpaRepository.findAllByUserId(userId).stream()
                .map(r -> Map.<String, Object>of(
                        "id",         r.getId(),
                        "name",       r.getName(),
                        "active",     r.isActive(),
                        "importedAt", r.getImportedAt()
                ))
                .toList();
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/activate")
    @Transactional
    public ResponseEntity<Map<String, Object>> activateRoutine(@PathVariable Long id) {
        Long userId = userResolver.currentUserId();

        // Deactivate all routines belonging to this user
        routineJpaRepository.deactivateAllByUserId(userId);

        // Activate the requested routine (ownership enforced by userId check)
        RoutineJpaEntity routine = routineJpaRepository.findAllByUserId(userId).stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new com.routineflow.application.usecase.exception.ResourceNotFoundException(
                        "Routine not found: " + id));

        routine.setActive(true);
        routineJpaRepository.save(routine);

        return ResponseEntity.ok(Map.of(
                "id",         routine.getId(),
                "name",       routine.getName(),
                "active",     routine.isActive(),
                "importedAt", routine.getImportedAt()
        ));
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("File must have an extension");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
