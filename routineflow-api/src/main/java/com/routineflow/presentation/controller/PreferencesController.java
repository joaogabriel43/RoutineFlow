package com.routineflow.presentation.controller;

import com.routineflow.application.dto.PreferencesResponse;
import com.routineflow.application.dto.UpdatePreferencesRequest;
import com.routineflow.application.usecase.PreferencesUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/preferences")
@Tag(name = "Preferences", description = "User preferences and settings endpoints")
public class PreferencesController {

    private final PreferencesUseCase preferencesUseCase;

    public PreferencesController(PreferencesUseCase preferencesUseCase) {
        this.preferencesUseCase = preferencesUseCase;
    }

    @GetMapping
    @Operation(summary = "Get user preferences", description = "Returns the active user's preferences, creating defaults if not found.")
    public ResponseEntity<PreferencesResponse> getPreferences() {
        return ResponseEntity.ok(preferencesUseCase.getPreferences());
    }

    @PutMapping
    @Operation(summary = "Update user preferences")
    public ResponseEntity<PreferencesResponse> updatePreferences(@Valid @RequestBody UpdatePreferencesRequest request) {
        return ResponseEntity.ok(preferencesUseCase.updatePreferences(request));
    }
}
