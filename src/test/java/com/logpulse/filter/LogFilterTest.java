package com.logpulse.filter;

import com.logpulse.model.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogFilterTest {

    private LogEntry infoEntry;
    private LogEntry errorEntry;
    private LogEntry warnEntry;

    @BeforeEach
    void setUp() {
        infoEntry  = new LogEntry(Instant.now(), "INFO",  "auth-service",  "User logged in successfully");
        errorEntry = new LogEntry(Instant.now(), "ERROR", "payment-service", "Payment gateway timeout");
        warnEntry  = new LogEntry(Instant.now(), "WARN",  "auth-service",  "Rate limit approaching");
    }

    @Test
    void filterByLevel_returnsOnlyMatchingLevel() {
        LogFilter filter = new LogFilter().withLevel("ERROR");
        List<LogEntry> result = filter.apply(List.of(infoEntry, errorEntry, warnEntry));
        assertEquals(1, result.size());
        assertEquals("ERROR", result.get(0).getLevel());
    }

    @Test
    void filterByService_returnsOnlyMatchingService() {
        LogFilter filter = new LogFilter().withService("auth-service");
        List<LogEntry> result = filter.apply(List.of(infoEntry, errorEntry, warnEntry));
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(e -> "auth-service".equalsIgnoreCase(e.getService())));
    }

    @Test
    void filterByMessageKeyword_isCaseInsensitive() {
        LogFilter filter = new LogFilter().withMessageContaining("timeout");
        List<LogEntry> result = filter.apply(List.of(infoEntry, errorEntry, warnEntry));
        assertEquals(1, result.size());
        assertTrue(result.get(0).getMessage().toLowerCase().contains("timeout"));
    }

    @Test
    void filterByMinSeverity_excludesBelowThreshold() {
        LogFilter filter = new LogFilter()
                .withMinSeverity(LogLevel.orderedNames(), "WARN");
        List<LogEntry> result = filter.apply(List.of(infoEntry, errorEntry, warnEntry));
        assertEquals(2, result.size());
        assertTrue(result.stream().noneMatch(e -> "INFO".equalsIgnoreCase(e.getLevel())));
    }

    @Test
    void composedFilters_applyAllPredicates() {
        LogFilter filter = new LogFilter()
                .withService("auth-service")
                .withLevel("WARN");
        List<LogEntry> result = filter.apply(List.of(infoEntry, errorEntry, warnEntry));
        assertEquals(1, result.size());
        assertEquals("WARN", result.get(0).getLevel());
    }

    @Test
    void applyOnNullList_returnsEmptyList() {
        LogFilter filter = new LogFilter().withLevel("INFO");
        List<LogEntry> result = filter.apply(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void reset_clearsAllPredicates() {
        LogFilter filter = new LogFilter().withLevel("ERROR");
        filter.reset();
        List<LogEntry> result = filter.apply(List.of(infoEntry, errorEntry, warnEntry));
        assertEquals(3, result.size());
    }
}
