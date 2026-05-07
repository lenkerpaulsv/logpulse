package com.logpulse.enrichment;

import java.util.Objects;
import java.util.function.Predicate;
import com.logpulse.model.LogEntry;

/**
 * Defines a rule that conditionally adds metadata fields to matching log entries.
 */
public class EnrichmentRule {

    private final String ruleName;
    private final Predicate<LogEntry> matcher;
    private final String fieldKey;
    private final String fieldValue;

    public EnrichmentRule(String ruleName, Predicate<LogEntry> matcher,
                          String fieldKey, String fieldValue) {
        Objects.requireNonNull(ruleName, "ruleName must not be null");
        Objects.requireNonNull(matcher, "matcher must not be null");
        Objects.requireNonNull(fieldKey, "fieldKey must not be null");
        Objects.requireNonNull(fieldValue, "fieldValue must not be null");
        this.ruleName = ruleName;
        this.matcher = matcher;
        this.fieldKey = fieldKey;
        this.fieldValue = fieldValue;
    }

    public String getRuleName() {
        return ruleName;
    }

    public boolean matches(LogEntry entry) {
        return matcher.test(entry);
    }

    public String getFieldKey() {
        return fieldKey;
    }

    public String getFieldValue() {
        return fieldValue;
    }

    @Override
    public String toString() {
        return "EnrichmentRule{name='" + ruleName + "', field='" + fieldKey + "'='" + fieldValue + "'}";
    }
}
