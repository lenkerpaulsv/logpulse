package com.logpulse.filter;

import java.util.List;

/**
 * Standard log levels in ascending order of severity.
 */
public enum LogLevel {
    TRACE, DEBUG, INFO, WARN, ERROR, FATAL;

    /**
     * Returns an immutable ordered list of level names (lowest to highest severity).
     */
    public static List<String> orderedNames() {
        return List.of(
                TRACE.name(),
                DEBUG.name(),
                INFO.name(),
                WARN.name(),
                ERROR.name(),
                FATAL.name()
        );
    }

    /**
     * Returns true if this level is at least as severe as the given minimum level.
     */
    public boolean isAtLeast(LogLevel minimum) {
        return this.ordinal() >= minimum.ordinal();
    }

    /**
     * Parse a string to a LogLevel, case-insensitive.
     * Returns null if the string does not match any level.
     */
    public static LogLevel parse(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LogLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
