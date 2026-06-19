package com.routineflow.application.usecase.validation;

import java.util.regex.Pattern;

/**
 * Validates the optional appearance fields (icon name + hex color) shared by
 * areas and tasks. Lives in the use-case layer so unit tests that call use cases
 * directly (bypassing controller bean-validation) still exercise the rules.
 *
 * Rules (Sprint 25):
 * - color: when non-null, must match #RRGGBB (case-insensitive).
 * - icon: when non-null, at most 50 chars. The icon name is NOT checked against
 *   the lucide catalog — the frontend guarantees validity; the backend only stores it.
 */
public final class AppearanceValidator {

    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final int MAX_ICON_LENGTH = 50;

    private AppearanceValidator() {
    }

    public static void validate(String icon, String color) {
        if (color != null && !HEX_COLOR.matcher(color).matches()) {
            throw new IllegalArgumentException("color must be a valid hex code like #2F8BFF");
        }
        if (icon != null && icon.length() > MAX_ICON_LENGTH) {
            throw new IllegalArgumentException("icon must be at most 50 characters");
        }
    }
}
