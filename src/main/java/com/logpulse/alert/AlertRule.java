package com.logpulse.alert;

import com.logpulse.filter.LogLevel;

import java.util.Objects;

/**
 * Defines a rule that triggers an alert when log conditions are met.
 */
public class AlertRule {

    private final String ruleId;
    private final String serviceName;
    private final LogLevel minimumLevel;
    private final int threshold;
    private final long windowSeconds;

    public AlertRule(String ruleId, String serviceName, LogLevel minimumLevel, int threshold, long windowSeconds) {
        if (ruleId == null || ruleId.isBlank()) throw new IllegalArgumentException("ruleId must not be blank");
        if (threshold <= 0) throw new IllegalArgumentException("threshold must be positive");
        if (windowSeconds <= 0) throw new IllegalArgumentException("windowSeconds must be positive");
        this.ruleId = ruleId;
        this.serviceName = serviceName;
        this.minimumLevel = Objects.requireNonNull(minimumLevel, "minimumLevel must not be null");
        this.threshold = threshold;
        this.windowSeconds = windowSeconds;
    }

    public String getRuleId() { return ruleId; }
    public String getServiceName() { return serviceName; }
    public LogLevel getMinimumLevel() { return minimumLevel; }
    public int getThreshold() { return threshold; }
    public long getWindowSeconds() { return windowSeconds; }

    @Override
    public String toString() {
        return String.format("AlertRule{id='%s', service='%s', level=%s, threshold=%d, window=%ds}",
                ruleId, serviceName, minimumLevel, threshold, windowSeconds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AlertRule)) return false;
        AlertRule that = (AlertRule) o;
        return Objects.equals(ruleId, that.ruleId);
    }

    @Override
    public int hashCode() { return Objects.hash(ruleId); }
}
