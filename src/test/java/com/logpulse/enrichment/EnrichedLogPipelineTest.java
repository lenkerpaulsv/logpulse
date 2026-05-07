package com.logpulse.enrichment;

import com.logpulse.filter.LogFilter;
import com.logpulse.filter.LogLevel;
import com.logpulse.model.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EnrichedLogPipelineTest {

    private List<LogEntry> output;
    private LogEnricher enricher;
    private LogFilter filter;

    @BeforeEach
    void setUp() {
        output = new ArrayList<>();
        enricher = new LogEnricher();
        filter = new LogFilter().withMinLevel(LogLevel.WARN);
    }

    private LogEntry entry(String service, LogLevel level) {
        return new LogEntry(Instant.now(), service, level, "msg");
    }

    @Test
    void onlyEnrichedEntriesReachDownstream() {
        EnrichedLogPipeline pipeline = new EnrichedLogPipeline(filter, enricher, output::add);

        pipeline.process(entry("svc", LogLevel.DEBUG));  // filtered out
        pipeline.process(entry("svc", LogLevel.WARN));   // passes
        pipeline.process(entry("svc", LogLevel.ERROR));  // passes

        assertEquals(2, output.size());
        assertEquals(3, pipeline.getProcessedCount());
        assertEquals(2, pipeline.getEnrichedCount());
    }

    @Test
    void enrichmentAppliedToPassingEntries() {
        enricher.addRule(new EnrichmentRule("flag",
                e -> e.getLevel() == LogLevel.ERROR, "alert", "yes"));

        EnrichedLogPipeline pipeline = new EnrichedLogPipeline(filter, enricher, output::add);
        pipeline.process(entry("svc", LogLevel.WARN));
        pipeline.process(entry("svc", LogLevel.ERROR));

        assertNull(output.get(0).getMetadata("alert"));
        assertEquals("yes", output.get(1).getMetadata("alert"));
    }

    @Test
    void processAllHandlesMultipleEntries() {
        EnrichedLogPipeline pipeline = new EnrichedLogPipeline(filter, enricher, output::add);
        List<LogEntry> entries = List.of(
                entry("a", LogLevel.INFO),
                entry("b", LogLevel.WARN),
                entry("c", LogLevel.ERROR));

        pipeline.processAll(entries);
        assertEquals(2, output.size());
    }

    @Test
    void resetClearsCounters() {
        EnrichedLogPipeline pipeline = new EnrichedLogPipeline(filter, enricher, output::add);
        pipeline.process(entry("svc", LogLevel.ERROR));
        assertEquals(1, pipeline.getProcessedCount());

        pipeline.reset();
        assertEquals(0, pipeline.getProcessedCount());
        assertEquals(0, pipeline.getEnrichedCount());
    }
}
