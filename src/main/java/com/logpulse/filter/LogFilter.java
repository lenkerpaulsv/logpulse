package com.logpulse.filter;

import com.logpulse.model.LogEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Composable filter for LogEntry objects.
 * Supports filtering by log level, service name, and message content.
 */
public class LogFilter {

    private final List<Predicate<LogEntry>> predicates = new ArrayList<>();

    public LogFilter withLevel(String level) {
        if (level != null && !level.isBlank()) {
            predicates.add(entry -> level.equalsIgnoreCase(entry.getLevel()));
        }
        return this;
    }

    public LogFilter withService(String serviceName) {
        if (serviceName != null && !serviceName.isBlank()) {
            predicates.add(entry -> serviceName.equalsIgnoreCase(entry.getService()));
        }
        return this;
    }

    public LogFilter withMessageContaining(String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            predicates.add(entry -> entry.getMessage() != null
                    && entry.getMessage().toLowerCase().contains(keyword.toLowerCase()));
        }
        return this;
    }

    public LogFilter withMinSeverity(List<String> orderedLevels, String minLevel) {
        if (minLevel != null && orderedLevels != null) {
            int minIndex = orderedLevels.indexOf(minLevel.toUpperCase());
            if (minIndex >= 0) {
                predicates.add(entry -> {
                    int idx = orderedLevels.indexOf(
                            entry.getLevel() != null ? entry.getLevel().toUpperCase() : "");
                    return idx >= minIndex;
                });
            }
        }
        return this;
    }

    public boolean matches(LogEntry entry) {
        if (entry == null) return false;
        return predicates.stream().allMatch(p -> p.test(entry));
    }

    public List<LogEntry> apply(List<LogEntry> entries) {
        if (entries == null) return List.of();
        return entries.stream().filter(this::matches).toList();
    }

    public void reset() {
        predicates.clear();
    }
}
