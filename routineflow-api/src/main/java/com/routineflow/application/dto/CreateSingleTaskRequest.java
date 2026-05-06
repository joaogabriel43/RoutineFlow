package com.routineflow.application.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CreateSingleTaskRequest(
        @NotBlank String title,
        String description,
        LocalDate dueDate   // nullable — past-date validation done in use case
) {}
