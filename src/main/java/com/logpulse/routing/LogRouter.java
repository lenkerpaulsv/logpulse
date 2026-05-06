package com.logpulse.routing;

import com.logpulse.model.LogEntry;
import com.logpulse.filter.LogLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Routes incoming log entries to registered consumers based on service name
 * and minimum log level. Supports wildcard routing for catch-all consumers.
 */
public class LogRouter {

    private static final String WILDCARD = "*";

    private final Map<String, List<RouteEntry>> routes = new ConcurrentHashMap<>();

    /**
     * Register a consumer for a specific service and minimum log level.
     *
     * @param service  service name or "*" for all services
     * @param minLevel minimum log level to route
     * @param consumer consumer to invoke with matching entries
     */
    public void register(String service, LogLevel minLevel, Consumer<LogEntry> consumer) {
        if (service == null || service.isBlank()) {
            throw new IllegalArgumentException("Service name must not be blank");
        }
        if (minLevel == null || consumer == null) {
            throw new IllegalArgumentException("minLevel and consumer must not be null");
        }
        routes.computeIfAbsent(service, k -> new ArrayList<>())
              .add(new RouteEntry(minLevel, consumer));
    }

    /**
     * Route a log entry to all matching registered consumers.
     *
     * @param entry the log entry to route
     */
    public void route(LogEntry entry) {
        if (entry == null) {
            return;
        }
        dispatch(entry, routes.getOrDefault(entry.getService(), List.of()));
        dispatch(entry, routes.getOrDefault(WILDCARD, List.of()));
    }

    /**
     * Remove all routes for a given service.
     *
     * @param service the service whose routes should be cleared
     */
    public void deregister(String service) {
        routes.remove(service);
    }

    /**
     * Returns the total number of registered route entries across all services.
     */
    public int routeCount() {
        return routes.values().stream().mapToInt(List::size).sum();
    }

    private void dispatch(LogEntry entry, List<RouteEntry> entries) {
        for (RouteEntry re : entries) {
            if (entry.getLevel().ordinal() >= re.minLevel().ordinal()) {
                re.consumer().accept(entry);
            }
        }
    }

    private record RouteEntry(LogLevel minLevel, Consumer<LogEntry> consumer) {}
}
