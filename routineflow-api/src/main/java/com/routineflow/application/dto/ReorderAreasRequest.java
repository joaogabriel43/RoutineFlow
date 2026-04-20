package com.routineflow.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReorderAreasRequest(

        @NotNull(message = "areaIds list is required")
        @Size(min = 1, message = "At least one area ID is required")
        List<Long> areaIds
) {}
