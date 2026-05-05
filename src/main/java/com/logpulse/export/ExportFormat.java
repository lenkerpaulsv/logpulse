package com.logpulse.export;

/**
 * Supported export formats for log entries.
 */
public enum ExportFormat {

    /**
     * Plain text format: [timestamp] [level] [service] message
     */
    PLAIN,

    /**
     * Comma-separated values with header row.
     */
    CSV,

    /**
     * JSON array of log entry objects.
     */
    JSON;

    /**
     * Resolves an ExportFormat from a string, case-insensitive.
     *
     * @param value the string representation
     * @return matching ExportFormat
     * @throws IllegalArgumentException if no match is found
     */
    public static ExportFormat fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Export format string must not be null or blank");
        }
        for (ExportFormat fmt : values()) {
            if (fmt.name().equalsIgnoreCase(value.trim())) {
                return fmt;
            }
        }
        throw new IllegalArgumentException("Unknown export format: '" + value + "'");
    }
}
