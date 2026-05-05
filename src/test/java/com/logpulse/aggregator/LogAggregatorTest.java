package com.logpulse.aggregator;

import com.logpulse.filter.LogFilter;
import com.logpulse.filter.LogLevel;
import com.logpulse.model.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogAggregatorTest {

    private LogAggregator aggregator;

    private static LogEntry entry(String service, LogLevel level, String message) {
        return new LogEntry(service, level, message, Instant.now());
    }

    @BeforeEach
    void setUp() {
        // Accept WARN and above
        LogFilter filter = new LogFilter(LogLevel.WARN);
        aggregator = new LogAggregator(filter, 5);
    }

    @Test
    void ingest_acceptsEntryThatPassesFilter() {
        LogEntry warn = entry("auth-service", LogLevel.WARN, "high latency");
        assertTrue(aggregator.ingest(warn));
        assertEquals(1, aggregator.tail("auth-service", 10).size());
    }

    @Test
    void ingest_rejectsEntryBelowFilterLevel() {
        LogEntry debug = entry("auth-service", LogLevel.DEBUG, "debug info");
        assertFalse(aggregator.ingest(debug));
        assertTrue(aggregator.tail("auth-service", 10).isEmpty());
    }

    @Test
    void ingest_rejectsNullEntry() {
        assertFalse(aggregator.ingest(null));
    }

    @Test
    void tail_returnsMostRecentNEntries() {
        for (int i = 1; i <= 4; i++) {
            aggregator.ingest(entry("api-gateway", LogLevel.ERROR, "error " + i));
        }
        List<LogEntry> tail = aggregator.tail("api-gateway", 2);
        assertEquals(2, tail.size());
        assertEquals("error 3", tail.get(0).getMessage());
        assertEquals("error 4", tail.get(1).getMessage());
    }

    @Test
    void tail_respectsBoundedBuffer() {
        for (int i = 1; i <= 8; i++) {
            aggregator.ingest(entry("worker", LogLevel.ERROR, "msg " + i));
        }
        // tailSize is 5, so only last 5 should be retained
        List<LogEntry> all = aggregator.tail("worker", 10);
        assertEquals(5, all.size());
        assertEquals("msg 4", all.get(0).getMessage());
        assertEquals("msg 8", all.get(4).getMessage());
    }

    @Test
    void tail_returnsEmptyForUnknownService() {
        assertTrue(aggregator.tail("unknown-service", 5).isEmpty());
    }

    @Test
    void allEntries_aggregatesAcrossServices() {
        aggregator.ingest(entry("svc-a", LogLevel.ERROR, "a error"));
        aggregator.ingest(entry("svc-b", LogLevel.WARN, "b warning"));
        aggregator.ingest(entry("svc-a", LogLevel.ERROR, "a error 2"));

        assertEquals(3, aggregator.allEntries().size());
        assertEquals(2, aggregator.trackedServices().size());
    }

    @Test
    void clear_removesAllBufferedEntries() {
        aggregator.ingest(entry("svc-a", LogLevel.ERROR, "some error"));
        aggregator.clear();
        assertTrue(aggregator.allEntries().isEmpty());
        assertTrue(aggregator.trackedServices().isEmpty());
    }

    @Test
    void constructor_throwsOnNonPositiveTailSize() {
        assertThrows(IllegalArgumentException.class,
                () -> new LogAggregator(new LogFilter(LogLevel.INFO), 0));
    }
}
