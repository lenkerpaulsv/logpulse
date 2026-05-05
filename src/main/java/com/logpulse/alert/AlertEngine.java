package com.logpulse.alert;

import com.logpulse.model.LogEntry;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Evaluates incoming log entries against registered AlertRules and fires AlertEvents
 * when thresholds are exceeded within the configured sliding time window.
 */
public class AlertEngine {

    private final List<AlertRule> rules = new CopyOnWriteArrayList<>();
    private final Map<String, Deque<Instant>> eventWindows = new ConcurrentHashMap<>();
    private final List<Consumer<AlertEvent>> listeners = new CopyOnWriteArrayList<>();

    public void registerRule(AlertRule rule) {
        Objects.requireNonNull(rule, "rule must not be null");
        rules.add(rule);
        eventWindows.putIfAbsent(rule.getRuleId(), new ArrayDeque<>());
    }

    public void addAlertListener(Consumer<AlertEvent> listener) {
        listeners.add(Objects.requireNonNull(listener, "listener must not be null"));
    }

    public void evaluate(LogEntry entry) {
        Objects.requireNonNull(entry, "entry must not be null");
        Instant now = entry.getTimestamp() != null ? entry.getTimestamp() : Instant.now();

        for (AlertRule rule : rules) {
            if (!matchesRule(rule, entry)) continue;

            Deque<Instant> window = eventWindows.get(rule.getRuleId());
            synchronized (window) {
                window.addLast(now);
                Instant cutoff = now.minusSeconds(rule.getWindowSeconds());
                while (!window.isEmpty() && window.peekFirst().isBefore(cutoff)) {
                    window.pollFirst();
                }
                if (window.size() >= rule.getThreshold()) {
                    AlertEvent event = new AlertEvent(rule, window.size(), now);
                    window.clear();
                    fireAlert(event);
                }
            }
        }
    }

    public List<AlertRule> getRules() { return Collections.unmodifiableList(rules); }

    private boolean matchesRule(AlertRule rule, LogEntry entry) {
        if (rule.getServiceName() != null && !rule.getServiceName().equals(entry.getServiceName())) {
            return false;
        }
        return entry.getLevel() != null && entry.getLevel().ordinal() >= rule.getMinimumLevel().ordinal();
    }

    private void fireAlert(AlertEvent event) {
        for (Consumer<AlertEvent> listener : listeners) {
            listener.accept(event);
        }
    }
}
