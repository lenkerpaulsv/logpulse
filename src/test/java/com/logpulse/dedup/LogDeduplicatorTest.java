package com.logpulse.dedup;

import com.logpulse.model.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class LogDeduplicatorTest {

    private LogDeduplicator deduplicator;
    private static final Duration WINDOW = Duration.ofSeconds(30);

    @BeforeEach
    void setUp() {
        deduplicator = new LogDeduplicator(WINDOW, 100);
    }

    private LogEntry entry(String service, String level, String message, Instant ts) {
        return new LogEntry(service, level, message, ts);
    }

    @Test
    void firstOccurrenceIsNotDuplicate() {
        LogEntry e = entry("auth", "ERROR", "Connection refused", Instant.now());
        assertFalse(deduplicator.isDuplicate(e));
    }

    @Test
    void sameEntryWithinWindowIsDuplicate() {
        Instant now = Instant.now();
        LogEntry first  = entry("auth", "ERROR", "Connection refused", now);
        LogEntry second = entry("auth", "ERROR", "Connection refused", now.plusSeconds(5));

        assertFalse(deduplicator.isDuplicate(first));
        assertTrue(deduplicator.isDuplicate(second));
        assertEquals(1, deduplicator.getTotalSuppressed());
    }

    @Test
    void entryAfterWindowExpiryIsNotDuplicate() {
        Instant now = Instant.now();
        LogEntry first  = entry("auth", "ERROR", "Timeout", now);
        LogEntry second = entry("auth", "ERROR", "Timeout", now.plus(WINDOW).plusSeconds(1));

        assertFalse(deduplicator.isDuplicate(first));
        assertFalse(deduplicator.isDuplicate(second));
        assertEquals(0, deduplicator.getTotalSuppressed());
    }

    @Test
    void differentServicesAreNotDuplicates() {
        Instant now = Instant.now();
        assertFalse(deduplicator.isDuplicate(entry("auth",    "ERROR", "Fail", now)));
        assertFalse(deduplicator.isDuplicate(entry("payment", "ERROR", "Fail", now)));
    }

    @Test
    void differentMessagesAreNotDuplicates() {
        Instant now = Instant.now();
        assertFalse(deduplicator.isDuplicate(entry("svc", "WARN", "Disk low",  now)));
        assertFalse(deduplicator.isDuplicate(entry("svc", "WARN", "Disk full", now)));
    }

    @Test
    void resetClearsCacheAndCounter() {
        Instant now = Instant.now();
        deduplicator.isDuplicate(entry("svc", "INFO", "Started", now));
        deduplicator.isDuplicate(entry("svc", "INFO", "Started", now.plusSeconds(1)));

        deduplicator.reset();
        assertEquals(0, deduplicator.getTotalSuppressed());
        assertEquals(0, deduplicator.getCacheSize());
    }

    @Test
    void constructorRejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> new LogDeduplicator(Duration.ZERO, 10));
        assertThrows(IllegalArgumentException.class, () -> new LogDeduplicator(Duration.ofSeconds(-1), 10));
        assertThrows(IllegalArgumentException.class, () -> new LogDeduplicator(WINDOW, 0));
    }

    @Test
    void nullEntryThrows() {
        assertThrows(NullPointerException.class, () -> deduplicator.isDuplicate(null));
    }
}
