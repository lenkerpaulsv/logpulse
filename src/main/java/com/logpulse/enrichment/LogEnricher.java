package com.logpulse.enrichment;

import com.logpulse.model.LogEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Applies a chain of {@link EnrichmentRule}s to log entries, augmenting
 * each entry's metadata with additional context fields.
 */
public class LogEnricher {

    private final List<EnrichmentRule> rules = new ArrayList<>();

    public LogEnricher addRule(EnrichmentRule rule) {
        Objects.requireNonNull(rule, "rule must not be null");
        rules.add(rule);
        return this;
    }

    public LogEnricher removeRule(String ruleName) {
        rules.removeIf(r -> r.getRuleName().equals(ruleName));
        return this;
    }

    /**
     * Enriches the given entry by applying all matching rules in order.
     *
     * @param entry the log entry to enrich (mutated in place via metadata)
     * @return the same entry with enriched metadata
     */
    public LogEntry enrich(LogEntry entry) {
        Objects.requireNonNull(entry, "entry must not be null");
        for (EnrichmentRule rule : rules) {
            if (rule.matches(entry)) {
                entry.addMetadata(rule.getFieldKey(), rule.getFieldValue());
            }
        }
        return entry;
    }

    /**
     * Enriches all entries in the provided list.
     */
    public List<LogEntry> enrichAll(List<LogEntry> entries) {
        Objects.requireNonNull(entries, "entries must not be null");
        entries.forEach(this::enrich);
        return entries;
    }

    public List<EnrichmentRule> getRules() {
        return Collections.unmodifiableList(rules);
    }

    public int getRuleCount() {
        return rules.size();
    }
}
