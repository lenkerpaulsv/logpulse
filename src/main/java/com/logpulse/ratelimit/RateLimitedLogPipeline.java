package com.logpulse.ratelimit;

import com.logpulse.model.LogEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A pipeline that wraps a LogRateLimiter and forwards allowed entries
 * to a downstream consumer, while tracking dropped entries.
 */
public class RateLimitedLogPipeline {

    private final LogRateLimiter rateLimiter;
    private final Consumer<LogEntry> downstream;

    private long totalReceived = 0;
    private long totalDropped = 0;
    private final List<LogEntry> droppedEntries = new ArrayList<>();

    public RateLimitedLogPipeline(LogRateLimiter rateLimiter, Consumer<LogEntry> downstream) {
        if (rateLimiter == null) throw new IllegalArgumentException("rateLimiter must not be null");
        if (downstream == null) throw new IllegalArgumentException("downstream must not be null");
        this.rateLimiter = rateLimiter;
        this.downstream = downstream;
    }

    public void process(LogEntry entry) {
        if (entry == null) return;
        totalReceived++;
        if (rateLimiter.allow(entry)) {
            downstream.accept(entry);
        } else {
            totalDropped++;
            droppedEntries.add(entry);
        }
    }

    public void processAll(List<LogEntry> entries) {
        if (entries == null) return;
        for (LogEntry entry : entries) {
            process(entry);
        }
    }

    public long getTotalReceived() {
        return totalReceived;
    }

    public long getTotalDropped() {
        return totalDropped;
    }

    public List<LogEntry> getDroppedEntries() {
        return new ArrayList<>(droppedEntries);
    }

    public double getDropRate() {
        if (totalReceived == 0) return 0.0;
        return (double) totalDropped / totalReceived;
    }

    public void resetStats() {
        totalReceived = 0;
        totalDropped = 0;
        droppedEntries.clear();
        rateLimiter.reset();
    }
}
