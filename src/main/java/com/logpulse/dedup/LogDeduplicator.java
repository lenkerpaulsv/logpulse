package com.logpulse.dedup;

import com.logpulse.model.LogEntry;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Deduplicates log entries within a configurable time window.
 * Uses a fingerprint based on service, level, and message to identify duplicates.
 */
public class LogDeduplicator {

    private final Duration window;
    private final int maxCacheSize;

    // fingerprint -> first-seen timestamp
    private final LinkedHashMap<String, Instant> seen;

    private long totalSuppressed = 0L;

    public LogDeduplicator(Duration window, int maxCacheSize) {
        if (window == null || window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("Window must be a positive duration");
        }
        if (maxCacheSize <= 0) {
            throw new IllegalArgumentException("maxCacheSize must be positive");
        }
        this.window = window;
        this.maxCacheSize = maxCacheSize;
        this.seen = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Instant> eldest) {
                return size() > maxCacheSize;
            }
        };
    }

    /**
     * Returns {@code true} if the entry is a duplicate and should be suppressed.
     */
    public synchronized boolean isDuplicate(LogEntry entry) {
        Objects.requireNonNull(entry, "entry must not be null");
        evictExpired(entry.getTimestamp());
        String fp = fingerprint(entry);
        if (seen.containsKey(fp)) {
            totalSuppressed++;
            return true;
        }
        seen.put(fp, entry.getTimestamp());
        return false;
    }

    public synchronized long getTotalSuppressed() {
        return totalSuppressed;
    }

    public synchronized int getCacheSize() {
        return seen.size();
    }

    public synchronized void reset() {
        seen.clear();
        totalSuppressed = 0L;
    }

    private void evictExpired(Instant now) {
        Instant cutoff = now.minus(window);
        seen.entrySet().removeIf(e -> e.getValue().isBefore(cutoff));
    }

    private String fingerprint(LogEntry entry) {
        return entry.getService() + "\0" + entry.getLevel() + "\0" + entry.getMessage();
    }
}
