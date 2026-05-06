package com.logpulse.sampling;

import com.logpulse.aggregator.LogAggregator;
import com.logpulse.model.LogEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * A lightweight pipeline that wraps a {@link LogSampler} and forwards
 * accepted entries to a downstream {@link LogAggregator} as well as any
 * registered {@link Consumer} listeners.
 *
 * <p>This is the primary integration point between the sampling module and
 * the rest of the logpulse pipeline.</p>
 */
public class SampledLogPipeline {

    private final LogSampler sampler;
    private final LogAggregator aggregator;
    private final List<Consumer<LogEntry>> listeners = new ArrayList<>();

    private long totalSeen = 0;
    private long totalAccepted = 0;

    public SampledLogPipeline(LogSampler sampler, LogAggregator aggregator) {
        this.sampler = sampler;
        this.aggregator = aggregator;
    }

    /**
     * Registers an additional listener that receives every <em>accepted</em> entry.
     */
    public void addListener(Consumer<LogEntry> listener) {
        listeners.add(listener);
    }

    /**
     * Submits a log entry to the pipeline.
     * The entry is forwarded downstream only when the sampler accepts it.
     *
     * @param entry the incoming log entry
     * @return {@code true} if the entry was accepted and forwarded
     */
    public boolean submit(LogEntry entry) {
        totalSeen++;
        if (!sampler.sample(entry)) {
            return false;
        }
        totalAccepted++;
        aggregator.add(entry);
        listeners.forEach(l -> l.accept(entry));
        return true;
    }

    /** Returns the total number of entries submitted to this pipeline. */
    public long getTotalSeen() {
        return totalSeen;
    }

    /** Returns the number of entries that passed the sampler. */
    public long getTotalAccepted() {
        return totalAccepted;
    }

    /**
     * Returns the acceptance ratio as a value in [0, 1],
     * or 0 if no entries have been seen yet.
     */
    public double getAcceptanceRatio() {
        return totalSeen == 0 ? 0.0 : (double) totalAccepted / totalSeen;
    }

    public LogSampler getSampler() {
        return sampler;
    }

    public List<Consumer<LogEntry>> getListeners() {
        return Collections.unmodifiableList(listeners);
    }
}
