package com.logpulse.tail;

import com.logpulse.filter.LogFilter;
import com.logpulse.model.LogEntry;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages multiple {@link LogTailer} instances, keyed by a service name.
 * Provides a single point to start, stop, and broadcast listeners across
 * all registered tailers.
 */
public class TailerRegistry implements Closeable {

    private final Map<String, LogTailer> tailers = new ConcurrentHashMap<>();

    /**
     * Registers and starts a tailer for the given service.
     *
     * @param serviceName unique name for the service
     * @param filePath    path to the log file
     * @param filter      optional filter (may be null)
     * @param listener    callback invoked for each matching log entry
     */
    public void register(String serviceName, Path filePath, LogFilter filter,
                         Consumer<LogEntry> listener) throws IOException {
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("serviceName must not be blank");
        }
        if (tailers.containsKey(serviceName)) {
            throw new IllegalStateException("Service already registered: " + serviceName);
        }
        LogTailer tailer = new LogTailer(filePath, filter);
        if (listener != null) {
            tailer.addListener(listener);
        }
        tailer.start();
        tailers.put(serviceName, tailer);
    }

    public void deregister(String serviceName) {
        LogTailer tailer = tailers.remove(serviceName);
        if (tailer != null) {
            tailer.stop();
        }
    }

    public boolean isRegistered(String serviceName) {
        return tailers.containsKey(serviceName);
    }

    public Collection<String> registeredServices() {
        return Collections.unmodifiableSet(tailers.keySet());
    }

    /** Adds an additional listener to an already-registered tailer. */
    public void addListener(String serviceName, Consumer<LogEntry> listener) {
        LogTailer tailer = tailers.get(serviceName);
        if (tailer == null) {
            throw new IllegalArgumentException("No tailer registered for service: " + serviceName);
        }
        tailer.addListener(listener);
    }

    @Override
    public void close() {
        tailers.values().forEach(LogTailer::stop);
        tailers.clear();
    }
}
