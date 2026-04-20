package com.routineflow.infrastructure.parser;

import com.routineflow.application.dto.ParsedRoutine;

import java.io.InputStream;

public interface RoutineFileParser {

    boolean supports(String fileExtension);

    ParsedRoutine parse(InputStream inputStream) throws RoutineParseException;
}
