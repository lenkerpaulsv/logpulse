package com.logpulse.stats;

import com.logpulse.filter.LogLevel;
import com.logpulse.model.LogEntry;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks real-time statistics for ingested log entries,
 * including counts per level and per service.
 */
public class LogStatistics {

    private final Map<LogLevel, AtomicLong> countsByLevel;
    private final Map<String, AtomicLong> countsByService;
    private final AtomicLong totalCount;

    public LogStatistics() {
        countsByLevel = new EnumMap<>(LogLevel.class);
        for (LogLevel level : LogLevel.values()) {
            countsByLevel.put(level, new AtomicLong(0));
        }
        countsByService = new HashMap<>();
        totalCount = new AtomicLong(0);
    }

    /**
     * Records a single log entry into the statistics.
     *
     * @param entry the log entry to record
     */
    public void record(LogEntry entry) {
        if (entry == null) {
            return;
        }
        totalCount.incrementAndGet();

        LogLevel level = entry.getLevel();
        if (level != null) {
            countsByLevel.get(level).incrementAndGet();
        }

        String service = entry.getService();
        if (service != null && !service.isBlank()) {
            countsByService
                    .computeIfAbsent(service, k -> new AtomicLong(0))
                    .incrementAndGet();
        }
    }

    public long getTotalCount() {
        return totalCount.get();
    }

    public long getCountForLevel(LogLevel level) {
        AtomicLong counter = countsByLevel.get(level);
        return counter != null ? counter.get() : 0L;
    }

    public long getCountForService(String service) {
        AtomicLong counter = countsByService.get(service);
        return counter != null ? counter.get() : 0L;
    }

    public Map<LogLevel, Long> getCountsByLevel() {
        Map<LogLevel, Long> snapshot = new EnumMap<>(LogLevel.class);
        countsByLevel.forEach((level, count) -> snapshot.put(level, count.get()));
        return Collections.unmodifiableMap(snapshot);
    }

    public Map<String, Long> getCountsByService() {
        Map<String, Long> snapshot = new HashMap<>();
        countsByService.forEach((service, count) -> snapshot.put(service, count.get()));
        return Collections.unmodifiableMap(snapshot);
    }

    /** Resets all counters to zero. */
    public void reset() {
        totalCount.set(0);
        countsByLevel.values().forEach(c -> c.set(0));
        countsByService.clear();
    }
}
