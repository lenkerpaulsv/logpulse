package com.logpulse.ratelimit;

import com.logpulse.model.LogEntry;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter that restricts the number of log entries processed per service
 * within a configurable time window (in seconds).
 */
public class LogRateLimiter {

    private final int maxEntriesPerWindow;
    private final long windowSeconds;

    private final Map<String, AtomicInteger> counts = new ConcurrentHashMap<>();
    private final Map<String, Long> windowStart = new ConcurrentHashMap<>();

    public LogRateLimiter(int maxEntriesPerWindow, long windowSeconds) {
        if (maxEntriesPerWindow <= 0) throw new IllegalArgumentException("maxEntriesPerWindow must be positive");
        if (windowSeconds <= 0) throw new IllegalArgumentException("windowSeconds must be positive");
        this.maxEntriesPerWindow = maxEntriesPerWindow;
        this.windowSeconds = windowSeconds;
    }

    /**
     * Returns true if the log entry is allowed through, false if it is rate-limited.
     */
    public boolean allow(LogEntry entry) {
        if (entry == null) return false;
        String service = entry.getService();
        long now = Instant.now().getEpochSecond();

        windowStart.putIfAbsent(service, now);
        counts.putIfAbsent(service, new AtomicInteger(0));

        long start = windowStart.get(service);
        if (now - start >= windowSeconds) {
            // Reset window
            windowStart.put(service, now);
            counts.get(service).set(0);
        }

        int current = counts.get(service).incrementAndGet();
        return current <= maxEntriesPerWindow;
    }

    /**
     * Returns the current count of entries processed for a service in the active window.
     */
    public int getCurrentCount(String service) {
        AtomicInteger counter = counts.get(service);
        return counter == null ? 0 : counter.get();
    }

    /**
     * Resets all rate limit state (useful for testing).
     */
    public void reset() {
        counts.clear();
        windowStart.clear();
    }

    public int getMaxEntriesPerWindow() {
        return maxEntriesPerWindow;
    }

    public long getWindowSeconds() {
        return windowSeconds;
    }
}
