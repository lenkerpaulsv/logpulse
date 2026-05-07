package com.logpulse.transform;

import com.logpulse.model.LogEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * LogTransformer applies a chain of transformation functions to LogEntry objects.
 * Transformations are applied in registration order and may modify fields such as
 * message, service name, or metadata tags.
 */
public class LogTransformer {

    private final List<Function<LogEntry, LogEntry>> transformations = new ArrayList<>();

    /**
     * Registers a transformation function to the pipeline.
     *
     * @param transformation a non-null function that accepts and returns a LogEntry
     */
    public void register(Function<LogEntry, LogEntry> transformation) {
        Objects.requireNonNull(transformation, "Transformation must not be null");
        transformations.add(transformation);
    }

    /**
     * Applies all registered transformations to the given log entry in order.
     *
     * @param entry the original LogEntry
     * @return the transformed LogEntry, or null if any transformation returns null
     */
    public LogEntry transform(LogEntry entry) {
        if (entry == null) {
            return null;
        }
        LogEntry current = entry;
        for (Function<LogEntry, LogEntry> fn : transformations) {
            current = fn.apply(current);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    /**
     * Applies all registered transformations to a batch of log entries.
     * Entries that become null after transformation are excluded from the result.
     *
     * @param entries list of LogEntry objects to transform
     * @return list of successfully transformed LogEntry objects
     */
    public List<LogEntry> transformAll(List<LogEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return new ArrayList<>();
        }
        List<LogEntry> results = new ArrayList<>();
        for (LogEntry entry : entries) {
            LogEntry transformed = transform(entry);
            if (transformed != null) {
                results.add(transformed);
            }
        }
        return results;
    }

    /**
     * Returns the number of registered transformations.
     *
     * @return transformation count
     */
    public int transformationCount() {
        return transformations.size();
    }

    /**
     * Clears all registered transformations.
     */
    public void clear() {
        transformations.clear();
    }
}
