package com.logpulse.sampling;

import com.logpulse.filter.LogLevel;
import com.logpulse.model.LogEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class LogSamplerTest {

    private LogEntry entry(LogLevel level) {
        return new LogEntry("svc", level, "msg-" + level, Instant.now());
    }

    // ── RATE_BASED ────────────────────────────────────────────────────────────

    @Test
    void rateBased_keepsEveryNthEntry() {
        LogSampler sampler = new LogSampler(SamplingStrategy.RATE_BASED, 5);
        List<Boolean> results = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> sampler.sample(entry(LogLevel.INFO)))
                .collect(Collectors.toList());

        // positions 5 and 10 (1-indexed) should be true
        assertEquals(2, results.stream().filter(b -> b).count());
        assertTrue(results.get(4));   // index 4 => 5th call
        assertTrue(results.get(9));   // index 9 => 10th call
    }

    @Test
    void rateBased_resetRestoresCounter() {
        LogSampler sampler = new LogSampler(SamplingStrategy.RATE_BASED, 3);
        sampler.sample(entry(LogLevel.DEBUG));
        sampler.sample(entry(LogLevel.DEBUG));
        sampler.reset();
        // After reset the 3rd call should be the first accepted one
        assertFalse(sampler.sample(entry(LogLevel.DEBUG))); // 1
        assertFalse(sampler.sample(entry(LogLevel.DEBUG))); // 2
        assertTrue(sampler.sample(entry(LogLevel.DEBUG)));  // 3
    }

    // ── PROBABILISTIC ────────────────────────────────────────────────────────

    @Test
    void probabilistic_acceptsAllWithProbabilityOne() {
        LogSampler sampler = new LogSampler(SamplingStrategy.PROBABILISTIC, 1.0);
        for (int i = 0; i < 50; i++) {
            assertTrue(sampler.sample(entry(LogLevel.INFO)));
        }
    }

    @Test
    void probabilistic_rejectsAllWithProbabilityZero() {
        // probability must be > 0 per constructor, so use a tiny epsilon instead
        // and verify near-zero acceptance over many samples
        LogSampler sampler = new LogSampler(SamplingStrategy.PROBABILISTIC, 0.001);
        long accepted = IntStream.range(0, 10_000)
                .filter(i -> sampler.sample(entry(LogLevel.DEBUG)))
                .count();
        assertTrue(accepted < 100, "Expected very few accepted entries, got " + accepted);
    }

    // ── PRIORITY_AWARE ───────────────────────────────────────────────────────

    @Test
    void priorityAware_alwaysAcceptsErrors() {
        LogSampler sampler = new LogSampler(SamplingStrategy.PRIORITY_AWARE, 0.001);
        for (int i = 0; i < 100; i++) {
            assertTrue(sampler.sample(entry(LogLevel.ERROR)));
            assertTrue(sampler.sample(entry(LogLevel.FATAL)));
        }
    }

    @Test
    void priorityAware_samplesLowerLevels() {
        LogSampler sampler = new LogSampler(SamplingStrategy.PRIORITY_AWARE, 1.0);
        // With probability 1.0, all lower-level entries should pass
        for (int i = 0; i < 20; i++) {
            assertTrue(sampler.sample(entry(LogLevel.INFO)));
        }
    }

    // ── constructor guard ────────────────────────────────────────────────────

    @Test
    void constructor_throwsOnNonPositiveRate() {
        assertThrows(IllegalArgumentException.class,
                () -> new LogSampler(SamplingStrategy.RATE_BASED, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new LogSampler(SamplingStrategy.PROBABILISTIC, -0.5));
    }

    @Test
    void getters_returnConfiguredValues() {
        LogSampler sampler = new LogSampler(SamplingStrategy.PROBABILISTIC, 0.25);
        assertEquals(SamplingStrategy.PROBABILISTIC, sampler.getStrategy());
        assertEquals(0.25, sampler.getRate(), 1e-9);
    }
}
