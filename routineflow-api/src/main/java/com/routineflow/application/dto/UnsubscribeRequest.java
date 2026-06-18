package com.routineflow.application.dto;

import jakarta.validation.constraints.NotBlank;

public record UnsubscribeRequest(
        @NotBlank(message = "endpoint is required")
        String endpoint
) {}
