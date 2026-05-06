package com.logpulse.correlation;

import com.logpulse.model.LogEntry;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks and correlates log entries across services using a correlation ID
 * extracted from each {@link LogEntry}'s metadata. Stale contexts older than
 * a configurable TTL are evicted on each ingestion call.
 */
public class LogCorrelator {

    private static final String CORRELATION_KEY = "correlationId";

    private final Map<String, CorrelationContext> contexts = new ConcurrentHashMap<>();
    private final Duration contextTtl;

    public LogCorrelator(Duration contextTtl) {
        if (contextTtl == null || contextTtl.isNegative() || contextTtl.isZero()) {
            throw new IllegalArgumentException("contextTtl must be a positive duration");
        }
        this.contextTtl = contextTtl;
    }

    /**
     * Ingests a log entry, updating the matching correlation context if a
     * correlationId is present in the entry's metadata.
     *
     * @return the updated {@link CorrelationContext}, or empty if no correlation ID found
     */
    public Optional<CorrelationContext> ingest(LogEntry entry) {
        evictStale();
        if (entry == null) {
            return Optional.empty();
        }
        String correlationId = entry.getMetadata() == null
                ? null
                : entry.getMetadata().get(CORRELATION_KEY);
        if (correlationId == null || correlationId.isBlank()) {
            return Optional.empty();
        }
        CorrelationContext ctx = contexts.computeIfAbsent(
                correlationId, CorrelationContext::new);
        ctx.record(entry.getServiceName());
        return Optional.of(ctx);
    }

    public Optional<CorrelationContext> getContext(String correlationId) {
        return Optional.ofNullable(contexts.get(correlationId));
    }

    public Collection<CorrelationContext> getAllContexts() {
        return Collections.unmodifiableCollection(contexts.values());
    }

    public int activeContextCount() {
        return contexts.size();
    }

    private void evictStale() {
        Instant cutoff = Instant.now().minus(contextTtl);
        contexts.values().removeIf(ctx -> ctx.getLastSeen().isBefore(cutoff));
    }
}
