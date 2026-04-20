package com.routineflow.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReorderTasksRequest(

        @NotNull(message = "taskIds list is required")
        @Size(min = 1, message = "At least one task ID is required")
        List<Long> taskIds
) {}
