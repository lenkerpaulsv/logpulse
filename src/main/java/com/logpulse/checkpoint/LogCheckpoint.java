package com.logpulse.checkpoint;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a saved position (byte offset) for a specific log source,
 * allowing the tailer to resume reading without reprocessing old entries.
 */
public class LogCheckpoint {

    private final String sourceId;
    private final long byteOffset;
    private final Instant savedAt;

    public LogCheckpoint(String sourceId, long byteOffset, Instant savedAt) {
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException("sourceId must not be blank");
        }
        if (byteOffset < 0) {
            throw new IllegalArgumentException("byteOffset must be non-negative");
        }
        this.sourceId = sourceId;
        this.byteOffset = byteOffset;
        this.savedAt = Objects.requireNonNull(savedAt, "savedAt must not be null");
    }

    public String getSourceId() {
        return sourceId;
    }

    public long getByteOffset() {
        return byteOffset;
    }

    public Instant getSavedAt() {
        return savedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LogCheckpoint)) return false;
        LogCheckpoint that = (LogCheckpoint) o;
        return byteOffset == that.byteOffset
                && Objects.equals(sourceId, that.sourceId)
                && Objects.equals(savedAt, that.savedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceId, byteOffset, savedAt);
    }

    @Override
    public String toString() {
        return "LogCheckpoint{sourceId='" + sourceId + "', byteOffset=" + byteOffset
                + ", savedAt=" + savedAt + "}";
    }
}
