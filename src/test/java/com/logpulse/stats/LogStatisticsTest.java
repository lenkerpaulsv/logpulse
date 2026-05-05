package com.logpulse.stats;

import com.logpulse.filter.LogLevel;
import com.logpulse.model.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LogStatisticsTest {

    private LogStatistics stats;

    @BeforeEach
    void setUp() {
        stats = new LogStatistics();
    }

    private LogEntry entry(String service, LogLevel level) {
        return new LogEntry(Instant.now(), level, service, "test message");
    }

    @Test
    void initialCountsAreZero() {
        assertEquals(0, stats.getTotalCount());
        for (LogLevel level : LogLevel.values()) {
            assertEquals(0, stats.getCountForLevel(level));
        }
    }

    @Test
    void recordIncrementsTotalCount() {
        stats.record(entry("auth-service", LogLevel.INFO));
        stats.record(entry("auth-service", LogLevel.WARN));
        assertEquals(2, stats.getTotalCount());
    }

    @Test
    void recordIncrementsLevelCount() {
        stats.record(entry("svc", LogLevel.ERROR));
        stats.record(entry("svc", LogLevel.ERROR));
        stats.record(entry("svc", LogLevel.INFO));

        assertEquals(2, stats.getCountForLevel(LogLevel.ERROR));
        assertEquals(1, stats.getCountForLevel(LogLevel.INFO));
        assertEquals(0, stats.getCountForLevel(LogLevel.DEBUG));
    }

    @Test
    void recordIncrementsServiceCount() {
        stats.record(entry("payment-service", LogLevel.INFO));
        stats.record(entry("payment-service", LogLevel.ERROR));
        stats.record(entry("order-service", LogLevel.WARN));

        assertEquals(2, stats.getCountForService("payment-service"));
        assertEquals(1, stats.getCountForService("order-service"));
        assertEquals(0, stats.getCountForService("unknown-service"));
    }

    @Test
    void getCountsByLevelReturnsSnapshot() {
        stats.record(entry("svc", LogLevel.DEBUG));
        stats.record(entry("svc", LogLevel.INFO));
        stats.record(entry("svc", LogLevel.INFO));

        Map<LogLevel, Long> byLevel = stats.getCountsByLevel();
        assertEquals(1L, byLevel.get(LogLevel.DEBUG));
        assertEquals(2L, byLevel.get(LogLevel.INFO));
    }

    @Test
    void getCountsByServiceReturnsSnapshot() {
        stats.record(entry("alpha", LogLevel.INFO));
        stats.record(entry("beta", LogLevel.INFO));
        stats.record(entry("alpha", LogLevel.WARN));

        Map<String, Long> byService = stats.getCountsByService();
        assertEquals(2L, byService.get("alpha"));
        assertEquals(1L, byService.get("beta"));
    }

    @Test
    void recordNullEntryIsIgnored() {
        assertDoesNotThrow(() -> stats.record(null));
        assertEquals(0, stats.getTotalCount());
    }

    @Test
    void resetClearsAllCounters() {
        stats.record(entry("svc", LogLevel.ERROR));
        stats.record(entry("svc", LogLevel.INFO));
        stats.reset();

        assertEquals(0, stats.getTotalCount());
        assertEquals(0, stats.getCountForLevel(LogLevel.ERROR));
        assertEquals(0, stats.getCountForService("svc"));
    }
}
