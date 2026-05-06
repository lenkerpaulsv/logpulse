package com.logpulse.correlation;

import com.logpulse.model.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LogCorrelatorTest {

    private LogCorrelator correlator;

    @BeforeEach
    void setUp() {
        correlator = new LogCorrelator(Duration.ofMinutes(5));
    }

    private LogEntry entryWithCorrelation(String correlationId, String service) {
        LogEntry entry = mock(LogEntry.class);
        when(entry.getMetadata()).thenReturn(Map.of("correlationId", correlationId));
        when(entry.getServiceName()).thenReturn(service);
        return entry;
    }

    private LogEntry entryWithoutCorrelation() {
        LogEntry entry = mock(LogEntry.class);
        when(entry.getMetadata()).thenReturn(Map.of());
        return entry;
    }

    @Test
    void constructor_throwsOnInvalidTtl() {
        assertThrows(IllegalArgumentException.class,
                () -> new LogCorrelator(Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new LogCorrelator(Duration.ofSeconds(-1)));
    }

    @Test
    void ingest_returnsEmptyWhenNoCorrelationId() {
        Optional<CorrelationContext> result = correlator.ingest(entryWithoutCorrelation());
        assertTrue(result.isEmpty());
    }

    @Test
    void ingest_returnsEmptyForNullEntry() {
        assertTrue(correlator.ingest(null).isEmpty());
    }

    @Test
    void ingest_createsContextOnFirstSeen() {
        Optional<CorrelationContext> result =
                correlator.ingest(entryWithCorrelation("trace-001", "api-gateway"));
        assertTrue(result.isPresent());
        assertEquals("trace-001", result.get().getCorrelationId());
        assertEquals(1, result.get().getEntryCount());
    }

    @Test
    void ingest_aggregatesAcrossMultipleServices() {
        correlator.ingest(entryWithCorrelation("trace-002", "api-gateway"));
        correlator.ingest(entryWithCorrelation("trace-002", "user-service"));
        correlator.ingest(entryWithCorrelation("trace-002", "db-service"));

        Optional<CorrelationContext> ctx = correlator.getContext("trace-002");
        assertTrue(ctx.isPresent());
        assertEquals(3, ctx.get().getEntryCount());
        assertTrue(ctx.get().spansMultipleServices());
        assertEquals(3, ctx.get().getServiceNames().size());
    }

    @Test
    void activeContextCount_reflectsDistinctIds() {
        correlator.ingest(entryWithCorrelation("id-1", "svc-a"));
        correlator.ingest(entryWithCorrelation("id-2", "svc-b"));
        correlator.ingest(entryWithCorrelation("id-1", "svc-c"));
        assertEquals(2, correlator.activeContextCount());
    }

    @Test
    void getAllContexts_isUnmodifiable() {
        correlator.ingest(entryWithCorrelation("id-x", "svc"));
        assertThrows(UnsupportedOperationException.class,
                () -> correlator.getAllContexts().clear());
    }
}
