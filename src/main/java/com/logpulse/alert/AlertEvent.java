package com.logpulse.alert;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a fired alert event produced when an AlertRule threshold is breached.
 */
public class AlertEvent {

    private final AlertRule rule;
    private final int count;
    private final Instant firedAt;
    private final String message;

    public AlertEvent(AlertRule rule, int count, Instant firedAt) {
        this.rule = Objects.requireNonNull(rule, "rule must not be null");
        this.count = count;
        this.firedAt = Objects.requireNonNull(firedAt, "firedAt must not be null");
        this.message = String.format(
                "ALERT [%s] Service '%s' exceeded %s threshold: %d occurrences in %ds window",
                rule.getRuleId(),
                rule.getServiceName() != null ? rule.getServiceName() : "*",
                rule.getMinimumLevel(),
                count,
                rule.getWindowSeconds()
        );
    }

    public AlertRule getRule() { return rule; }
    public int getCount() { return count; }
    public Instant getFiredAt() { return firedAt; }
    public String getMessage() { return message; }

    @Override
    public String toString() { return message; }
}
