package com.routineflow.presentation.controller;

import com.routineflow.application.dto.SkipDayRequest;
import com.routineflow.application.dto.SkipDayResponse;
import com.routineflow.application.usecase.SkipDayUseCase;
import com.routineflow.infrastructure.security.AuthenticatedUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/areas/{areaId}/skip-days")
@Tag(name = "Skip Days", description = "Gerenciamento de skip days (vacation mode) por área")
public class SkipDayController {

    private final SkipDayUseCase skipDayUseCase;
    private final AuthenticatedUserResolver userResolver;

    public SkipDayController(SkipDayUseCase skipDayUseCase, AuthenticatedUserResolver userResolver) {
        this.skipDayUseCase = skipDayUseCase;
        this.userResolver = userResolver;
    }

    @PostMapping
    @Operation(summary = "Adicionar skip day para a área")
    public ResponseEntity<SkipDayResponse> skipDay(
            @PathVariable Long areaId,
            @Valid @RequestBody SkipDayRequest request) {
        Long userId = userResolver.currentUserId();
        SkipDayResponse response = skipDayUseCase.skipDay(userId, areaId, request.date(), request.reason());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{date}")
    @Operation(summary = "Remover skip day da área")
    public ResponseEntity<Void> removeSkipDay(
            @PathVariable Long areaId,
            @PathVariable LocalDate date) {
        Long userId = userResolver.currentUserId();
        skipDayUseCase.removeSkipDay(userId, areaId, date);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Listar skip days da área")
    public ResponseEntity<List<SkipDayResponse>> listSkipDays(@PathVariable Long areaId) {
        Long userId = userResolver.currentUserId();
        return ResponseEntity.ok(skipDayUseCase.listSkipDays(userId, areaId));
    }
}
