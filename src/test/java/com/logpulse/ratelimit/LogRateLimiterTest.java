package com.logpulse.ratelimit;

import com.logpulse.model.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogRateLimiterTest {

    private LogRateLimiter rateLimiter;
    private List<LogEntry> accepted;

    @BeforeEach
    void setUp() {
        rateLimiter = new LogRateLimiter(3, 60);
        accepted = new ArrayList<>();
    }

    private LogEntry entry(String service) {
        return new LogEntry(service, "INFO", "test message", Instant.now());
    }

    @Test
    void allowsEntriesUpToLimit() {
        assertTrue(rateLimiter.allow(entry("auth-service")));
        assertTrue(rateLimiter.allow(entry("auth-service")));
        assertTrue(rateLimiter.allow(entry("auth-service")));
    }

    @Test
    void blocksEntriesExceedingLimit() {
        rateLimiter.allow(entry("auth-service"));
        rateLimiter.allow(entry("auth-service"));
        rateLimiter.allow(entry("auth-service"));
        assertFalse(rateLimiter.allow(entry("auth-service")));
        assertFalse(rateLimiter.allow(entry("auth-service")));
    }

    @Test
    void tracksCountsPerServiceIndependently() {
        rateLimiter.allow(entry("auth-service"));
        rateLimiter.allow(entry("auth-service"));
        rateLimiter.allow(entry("auth-service"));
        // Different service should still be allowed
        assertTrue(rateLimiter.allow(entry("payment-service")));
        assertTrue(rateLimiter.allow(entry("payment-service")));
    }

    @Test
    void resetClearsAllState() {
        rateLimiter.allow(entry("auth-service"));
        rateLimiter.allow(entry("auth-service"));
        rateLimiter.allow(entry("auth-service"));
        assertFalse(rateLimiter.allow(entry("auth-service")));
        rateLimiter.reset();
        assertTrue(rateLimiter.allow(entry("auth-service")));
    }

    @Test
    void pipelineTracksDroppedEntries() {
        RateLimitedLogPipeline pipeline = new RateLimitedLogPipeline(rateLimiter, accepted::add);
        for (int i = 0; i < 6; i++) {
            pipeline.process(entry("order-service"));
        }
        assertEquals(6, pipeline.getTotalReceived());
        assertEquals(3, pipeline.getTotalDropped());
        assertEquals(3, accepted.size());
        assertEquals(3, pipeline.getDroppedEntries().size());
    }

    @Test
    void pipelineDropRateCalculatedCorrectly() {
        RateLimitedLogPipeline pipeline = new RateLimitedLogPipeline(rateLimiter, accepted::add);
        for (int i = 0; i < 6; i++) {
            pipeline.process(entry("order-service"));
        }
        assertEquals(0.5, pipeline.getDropRate(), 0.001);
    }

    @Test
    void nullEntryIsRejectedGracefully() {
        assertFalse(rateLimiter.allow(null));
    }

    @Test
    void constructorRejectsInvalidParams() {
        assertThrows(IllegalArgumentException.class, () -> new LogRateLimiter(0, 60));
        assertThrows(IllegalArgumentException.class, () -> new LogRateLimiter(10, 0));
    }
}
