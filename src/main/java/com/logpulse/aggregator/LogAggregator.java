package com.logpulse.aggregator;

import com.logpulse.filter.LogFilter;
import com.logpulse.model.LogEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Aggregates log entries from multiple services, applying optional filters
 * and maintaining a bounded in-memory tail buffer per service.
 */
public class LogAggregator {

    private static final int DEFAULT_TAIL_SIZE = 100;

    private final int tailSize;
    private final LogFilter filter;
    private final Map<String, CopyOnWriteArrayList<LogEntry>> serviceBuffers;

    public LogAggregator(LogFilter filter, int tailSize) {
        if (tailSize <= 0) {
            throw new IllegalArgumentException("tailSize must be positive");
        }
        this.filter = filter;
        this.tailSize = tailSize;
        this.serviceBuffers = new ConcurrentHashMap<>();
    }

    public LogAggregator(LogFilter filter) {
        this(filter, DEFAULT_TAIL_SIZE);
    }

    /**
     * Ingest a log entry. The entry is accepted only if it passes the configured filter.
     *
     * @param entry the log entry to ingest
     * @return true if the entry was accepted and stored, false if filtered out
     */
    public boolean ingest(LogEntry entry) {
        if (entry == null || !filter.accepts(entry)) {
            return false;
        }
        CopyOnWriteArrayList<LogEntry> buffer = serviceBuffers
                .computeIfAbsent(entry.getServiceName(), k -> new CopyOnWriteArrayList<>());

        synchronized (buffer) {
            buffer.add(entry);
            while (buffer.size() > tailSize) {
                buffer.remove(0);
            }
        }
        return true;
    }

    /**
     * Returns the tail of log entries for a specific service.
     *
     * @param serviceName the service to query
     * @param n           number of most-recent entries to return
     * @return immutable list of the last n entries (or fewer if not enough exist)
     */
    public List<LogEntry> tail(String serviceName, int n) {
        CopyOnWriteArrayList<LogEntry> buffer = serviceBuffers.get(serviceName);
        if (buffer == null || buffer.isEmpty()) {
            return Collections.emptyList();
        }
        List<LogEntry> snapshot = new ArrayList<>(buffer);
        int fromIndex = Math.max(0, snapshot.size() - n);
        return Collections.unmodifiableList(snapshot.subList(fromIndex, snapshot.size()));
    }

    /**
     * Returns all entries across every known service, in ingestion order per service.
     */
    public List<LogEntry> allEntries() {
        return serviceBuffers.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toUnmodifiableList());
    }

    /** Returns the set of service names currently tracked. */
    public java.util.Set<String> trackedServices() {
        return Collections.unmodifiableSet(serviceBuffers.keySet());
    }

    /** Clears all buffered entries (useful for testing / reset scenarios). */
    public void clear() {
        serviceBuffers.clear();
    }
}
