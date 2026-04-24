package com.routineflow.application.dto;

import com.routineflow.domain.model.ResetFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAreaRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @NotBlank(message = "Color is required")
        @Pattern(regexp = "#[0-9A-Fa-f]{6}", message = "Color must be a valid hex code (e.g. #3B82F6)")
        String color,

        @NotBlank(message = "Icon is required")
        @Size(max = 10, message = "Icon must be at most 10 characters")
        String icon,

        // Nullable — UseCase defaults to DAILY when absent
        ResetFrequency resetFrequency
) {}
