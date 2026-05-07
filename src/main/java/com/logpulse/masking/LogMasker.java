package com.logpulse.masking;

import com.logpulse.model.LogEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Applies a set of {@link MaskingRule}s to {@link LogEntry} messages,
 * redacting sensitive information such as passwords, tokens, and PII
 * before logs are exported or stored.
 */
public class LogMasker {

    private final List<MaskingRule> rules;

    public LogMasker() {
        this.rules = new ArrayList<>();
    }

    public LogMasker(List<MaskingRule> rules) {
        this.rules = new ArrayList<>(rules);
    }

    /**
     * Registers a masking rule with this masker.
     *
     * @param rule the rule to add
     */
    public void addRule(MaskingRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("MaskingRule must not be null");
        }
        rules.add(rule);
    }

    /**
     * Applies all registered masking rules to the given log entry's message.
     * Returns a new {@link LogEntry} with the masked message; the original is unchanged.
     *
     * @param entry the log entry to mask
     * @return a new log entry with sensitive data redacted
     */
    public LogEntry mask(LogEntry entry) {
        if (entry == null) {
            return null;
        }
        String maskedMessage = entry.getMessage();
        for (MaskingRule rule : rules) {
            maskedMessage = rule.apply(maskedMessage);
        }
        return new LogEntry(
                entry.getTimestamp(),
                entry.getLevel(),
                entry.getService(),
                maskedMessage,
                entry.getTraceId()
        );
    }

    /**
     * Applies masking to a list of log entries.
     *
     * @param entries the entries to mask
     * @return a new list of masked log entries
     */
    public List<LogEntry> maskAll(List<LogEntry> entries) {
        if (entries == null) {
            return Collections.emptyList();
        }
        List<LogEntry> result = new ArrayList<>(entries.size());
        for (LogEntry entry : entries) {
            result.add(mask(entry));
        }
        return result;
    }

    public List<MaskingRule> getRules() {
        return Collections.unmodifiableList(rules);
    }

    public int getRuleCount() {
        return rules.size();
    }
}
