package com.routineflow.domain.model;

/** Controls how a new routine file is applied to an existing active routine. */
public enum ImportMode {
    /** Deactivate all previous routines and create a new one from the file (current behaviour). */
    REPLACE,
    /** Keep the active routine, add new areas and tasks; skip exact duplicates silently. */
    MERGE
}
