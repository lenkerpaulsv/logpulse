package com.logpulse.transform;

import com.logpulse.model.LogEntry;
import com.logpulse.filter.LogLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogTransformerTest {

    private LogTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new LogTransformer();
    }

    private LogEntry sampleEntry(String service, String message) {
        return new LogEntry(Instant.now(), LogLevel.INFO, service, message);
    }

    @Test
    void transformWithNoTransformationsReturnsOriginalEntry() {
        LogEntry entry = sampleEntry("auth-service", "User logged in");
        LogEntry result = transformer.transform(entry);
        assertSame(entry, result);
    }

    @Test
    void transformAppliesSingleTransformation() {
        transformer.register(e -> new LogEntry(e.getTimestamp(), e.getLevel(), e.getService().toUpperCase(), e.getMessage()));
        LogEntry entry = sampleEntry("auth-service", "User logged in");
        LogEntry result = transformer.transform(entry);
        assertNotNull(result);
        assertEquals("AUTH-SERVICE", result.getService());
    }

    @Test
    void transformAppliesMultipleTransformationsInOrder() {
        transformer.register(e -> new LogEntry(e.getTimestamp(), e.getLevel(), e.getService(), "[PREFIXED] " + e.getMessage()));
        transformer.register(e -> new LogEntry(e.getTimestamp(), e.getLevel(), e.getService(), e.getMessage() + " [SUFFIXED]"));
        LogEntry entry = sampleEntry("api-gateway", "Request received");
        LogEntry result = transformer.transform(entry);
        assertNotNull(result);
        assertEquals("[PREFIXED] Request received [SUFFIXED]", result.getMessage());
    }

    @Test
    void transformReturnsNullWhenTransformationReturnsNull() {
        transformer.register(e -> null);
        LogEntry entry = sampleEntry("payment-service", "Payment processed");
        LogEntry result = transformer.transform(entry);
        assertNull(result);
    }

    @Test
    void transformReturnsNullForNullInput() {
        transformer.register(e -> e);
        assertNull(transformer.transform(null));
    }

    @Test
    void transformAllFiltersOutNullResults() {
        transformer.register(e -> e.getMessage().contains("DROP") ? null : e);
        List<LogEntry> entries = List.of(
            sampleEntry("svc", "Keep this"),
            sampleEntry("svc", "DROP this"),
            sampleEntry("svc", "Keep this too")
        );
        List<LogEntry> results = transformer.transformAll(entries);
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(e -> e.getMessage().startsWith("Keep")));
    }

    @Test
    void transformAllReturnsEmptyListForEmptyInput() {
        List<LogEntry> results = transformer.transformAll(List.of());
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void registerNullTransformationThrowsException() {
        assertThrows(NullPointerException.class, () -> transformer.register(null));
    }

    @Test
    void transformationCountReflectsRegistrations() {
        assertEquals(0, transformer.transformationCount());
        transformer.register(e -> e);
        transformer.register(e -> e);
        assertEquals(2, transformer.transformationCount());
    }

    @Test
    void clearRemovesAllTransformations() {
        transformer.register(e -> e);
        transformer.register(e -> e);
        transformer.clear();
        assertEquals(0, transformer.transformationCount());
        LogEntry entry = sampleEntry("svc", "msg");
        assertSame(entry, transformer.transform(entry));
    }
}
