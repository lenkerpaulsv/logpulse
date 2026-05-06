package com.logpulse.sampling;

/**
 * Defines the strategy used to sample log entries.
 */
public enum SamplingStrategy {

    /**
     * Accept every Nth log entry (deterministic).
     */
    RATE_BASED,

    /**
     * Accept each log entry with a fixed probability (0.0 – 1.0).
     */
    PROBABILISTIC,

    /**
     * Always accept entries whose log level is ERROR or FATAL;
     * apply rate-based sampling to everything else.
     */
    PRIORITY_AWARE;

    /**
     * Returns true if the strategy requires a numeric rate/probability
     * parameter rather than a simple boolean toggle.
     */
    public boolean requiresRate() {
        return this == RATE_BASED || this == PROBABILISTIC || this == PRIORITY_AWARE;
    }
}
