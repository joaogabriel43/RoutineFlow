package com.routineflow.presentation.controller;

import com.routineflow.application.dto.AreaAnalyticsResponse;
import com.routineflow.application.dto.AreaResponse;
import com.routineflow.application.dto.CreateAreaRequest;
import com.routineflow.application.dto.ReorderAreasRequest;
import com.routineflow.application.dto.UpdateAreaRequest;
import com.routineflow.application.usecase.AreaAnalyticsUseCase;
import com.routineflow.application.usecase.AreaUseCase;
import com.routineflow.infrastructure.security.AuthenticatedUserResolver;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/areas")
public class AreaController {

    private final AreaUseCase areaUseCase;
    private final AreaAnalyticsUseCase areaAnalyticsUseCase;
    private final AuthenticatedUserResolver userResolver;

    public AreaController(
            AreaUseCase areaUseCase,
            AreaAnalyticsUseCase areaAnalyticsUseCase,
            AuthenticatedUserResolver userResolver
    ) {
        this.areaUseCase = areaUseCase;
        this.areaAnalyticsUseCase = areaAnalyticsUseCase;
        this.userResolver = userResolver;
    }

    @GetMapping
    public ResponseEntity<List<AreaResponse>> getAreas() {
        Long userId = userResolver.currentUserId();
        return ResponseEntity.ok(areaUseCase.getAreas(userId));
    }

    @PostMapping
    public ResponseEntity<AreaResponse> createArea(@Valid @RequestBody CreateAreaRequest request) {
        Long userId = userResolver.currentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(areaUseCase.createArea(userId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AreaResponse> updateArea(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAreaRequest request
    ) {
        Long userId = userResolver.currentUserId();
        return ResponseEntity.ok(areaUseCase.updateArea(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArea(@PathVariable Long id) {
        Long userId = userResolver.currentUserId();
        areaUseCase.deleteArea(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/reorder")
    public ResponseEntity<List<AreaResponse>> reorderAreas(
            @Valid @RequestBody ReorderAreasRequest request
    ) {
        Long userId = userResolver.currentUserId();
        return ResponseEntity.ok(areaUseCase.reorderAreas(userId, request));
    }

    @GetMapping("/{id}/analytics")
    public ResponseEntity<AreaAnalyticsResponse> getAreaAnalytics(@PathVariable Long id) {
        Long userId = userResolver.currentUserId();
        return ResponseEntity.ok(areaAnalyticsUseCase.getAreaAnalytics(userId, id));
    }
}
