package com.routineflow.application.dto;

import java.util.List;

public record ParsedRoutine(
        String name,
        List<ParsedArea> areas
) {}
