package com.logpulse.sampling;

import com.logpulse.filter.LogLevel;
import com.logpulse.model.LogEntry;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Samples incoming {@link LogEntry} objects according to a configurable
 * {@link SamplingStrategy}, reducing volume while preserving signal.
 */
public class LogSampler {

    private final SamplingStrategy strategy;
    /** For RATE_BASED: accept 1-in-N; for PROBABILISTIC: acceptance probability. */
    private final double rate;
    private final AtomicLong counter = new AtomicLong(0);

    /**
     * @param strategy the sampling strategy to apply
     * @param rate     for RATE_BASED: N (e.g. 5 means keep every 5th entry);
     *                 for PROBABILISTIC / PRIORITY_AWARE: probability in [0,1]
     */
    public LogSampler(SamplingStrategy strategy, double rate) {
        if (strategy.requiresRate() && rate <= 0) {
            throw new IllegalArgumentException("rate must be positive");
        }
        this.strategy = strategy;
        this.rate = rate;
    }

    /**
     * Returns {@code true} if the given entry should be kept.
     */
    public boolean sample(LogEntry entry) {
        switch (strategy) {
            case RATE_BASED:
                return (counter.incrementAndGet() % (long) rate) == 0;

            case PROBABILISTIC:
                return Math.random() < rate;

            case PRIORITY_AWARE:
                LogLevel level = entry.getLevel();
                if (level == LogLevel.ERROR || level == LogLevel.FATAL) {
                    return true;
                }
                return Math.random() < rate;

            default:
                return true;
        }
    }

    /** Resets the internal counter (useful between test runs or replay sessions). */
    public void reset() {
        counter.set(0);
    }

    public SamplingStrategy getStrategy() {
        return strategy;
    }

    public double getRate() {
        return rate;
    }
}
