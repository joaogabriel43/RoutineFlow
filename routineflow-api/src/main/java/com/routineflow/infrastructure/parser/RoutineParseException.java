package com.routineflow.infrastructure.parser;

public class RoutineParseException extends RuntimeException {

    public RoutineParseException(String message) {
        super(message);
    }

    public RoutineParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
