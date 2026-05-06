package com.logpulse.correlation;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * Holds a group of correlated log entries sharing the same correlation ID.
 */
public class CorrelationContext {

    private final String correlationId;
    private final List<String> serviceNames;
    private final Instant firstSeen;
    private Instant lastSeen;
    private int entryCount;

    public CorrelationContext(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException("correlationId must not be null or blank");
        }
        this.correlationId = correlationId;
        this.serviceNames = new ArrayList<>();
        this.firstSeen = Instant.now();
        this.lastSeen = this.firstSeen;
        this.entryCount = 0;
    }

    public void record(String serviceName) {
        if (serviceName != null && !serviceNames.contains(serviceName)) {
            serviceNames.add(serviceName);
        }
        entryCount++;
        lastSeen = Instant.now();
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public List<String> getServiceNames() {
        return Collections.unmodifiableList(serviceNames);
    }

    public Instant getFirstSeen() {
        return firstSeen;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public int getEntryCount() {
        return entryCount;
    }

    public boolean spansMultipleServices() {
        return serviceNames.size() > 1;
    }

    @Override
    public String toString() {
        return "CorrelationContext{id='" + correlationId + "', services=" + serviceNames +
                ", entries=" + entryCount + "}";
    }
}
