package com.logpulse.model;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single structured log entry parsed from a service.
 */
public class LogEntry {

    public enum Level {
        TRACE, DEBUG, INFO, WARN, ERROR, FATAL
    }

    private final Instant timestamp;
    private final Level level;
    private final String service;
    private final String message;
    private final Map<String, String> fields;

    public LogEntry(Instant timestamp, Level level, String service, String message, Map<String, String> fields) {
        if (timestamp == null) throw new IllegalArgumentException("timestamp must not be null");
        if (level == null)     throw new IllegalArgumentException("level must not be null");
        if (service == null || service.isBlank()) throw new IllegalArgumentException("service must not be blank");
        if (message == null)   throw new IllegalArgumentException("message must not be null");

        this.timestamp = timestamp;
        this.level     = level;
        this.service   = service;
        this.message   = message;
        this.fields    = fields != null ? Collections.unmodifiableMap(new HashMap<>(fields)) : Collections.emptyMap();
    }

    public Instant getTimestamp() { return timestamp; }
    public Level   getLevel()     { return level; }
    public String  getService()   { return service; }
    public String  getMessage()   { return message; }
    public Map<String, String> getFields() { return fields; }

    @Override
    public String toString() {
        return String.format("[%s] %s %-5s %s %s",
                timestamp, service, level, message,
                fields.isEmpty() ? "" : fields);
    }
}
