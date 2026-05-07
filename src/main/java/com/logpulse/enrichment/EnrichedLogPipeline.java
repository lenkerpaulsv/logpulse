package com.logpulse.enrichment;

import com.logpulse.filter.LogFilter;
import com.logpulse.model.LogEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Pipeline that filters log entries and then enriches matching ones before
 * forwarding them to a downstream consumer.
 */
public class EnrichedLogPipeline {

    private final LogFilter filter;
    private final LogEnricher enricher;
    private final Consumer<LogEntry> downstream;
    private int processedCount = 0;
    private int enrichedCount = 0;

    public EnrichedLogPipeline(LogFilter filter, LogEnricher enricher,
                               Consumer<LogEntry> downstream) {
        this.filter = Objects.requireNonNull(filter, "filter must not be null");
        this.enricher = Objects.requireNonNull(enricher, "enricher must not be null");
        this.downstream = Objects.requireNonNull(downstream, "downstream must not be null");
    }

    public void process(LogEntry entry) {
        Objects.requireNonNull(entry, "entry must not be null");
        processedCount++;
        if (filter.accepts(entry)) {
            enricher.enrich(entry);
            enrichedCount++;
            downstream.accept(entry);
        }
    }

    public void processAll(List<LogEntry> entries) {
        Objects.requireNonNull(entries, "entries must not be null");
        entries.forEach(this::process);
    }

    public int getProcessedCount() {
        return processedCount;
    }

    public int getEnrichedCount() {
        return enrichedCount;
    }

    public void reset() {
        processedCount = 0;
        enrichedCount = 0;
    }
}
