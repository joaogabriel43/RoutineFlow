package com.routineflow.infrastructure.config;

import java.time.ZoneId;

/**
 * Centralized timezone constant for the entire application.
 *
 * <p>Railway (and most cloud platforms) run with UTC as the JVM default. All
 * {@code LocalDate.now()} calls must use this zone so that "today" boundaries
 * align with Brasília time (UTC-3 / UTC-2 in summer).</p>
 *
 * <p>Usage: {@code LocalDate.now(AppTimeZone.ZONE)}</p>
 */
public final class AppTimeZone {

    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private AppTimeZone() {}
}
