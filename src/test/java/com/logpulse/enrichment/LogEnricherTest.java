package com.logpulse.enrichment;

import com.logpulse.filter.LogLevel;
import com.logpulse.model.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogEnricherTest {

    private LogEnricher enricher;

    @BeforeEach
    void setUp() {
        enricher = new LogEnricher();
    }

    private LogEntry entry(String service, LogLevel level) {
        return new LogEntry(Instant.now(), service, level, "test message");
    }

    @Test
    void enrichAddsFieldWhenRuleMatches() {
        enricher.addRule(new EnrichmentRule("tag-auth",
                e -> "auth-service".equals(e.getService()),
                "team", "identity"));

        LogEntry e = entry("auth-service", LogLevel.INFO);
        enricher.enrich(e);

        assertEquals("identity", e.getMetadata("team"));
    }

    @Test
    void enrichSkipsFieldWhenRuleDoesNotMatch() {
        enricher.addRule(new EnrichmentRule("tag-auth",
                e -> "auth-service".equals(e.getService()),
                "team", "identity"));

        LogEntry e = entry("billing-service", LogLevel.INFO);
        enricher.enrich(e);

        assertNull(e.getMetadata("team"));
    }

    @Test
    void multipleRulesAppliedInOrder() {
        enricher.addRule(new EnrichmentRule("env",
                e -> true, "env", "production"));
        enricher.addRule(new EnrichmentRule("critical",
                e -> e.getLevel() == LogLevel.ERROR,
                "priority", "high"));

        LogEntry e = entry("api", LogLevel.ERROR);
        enricher.enrich(e);

        assertEquals("production", e.getMetadata("env"));
        assertEquals("high", e.getMetadata("priority"));
    }

    @Test
    void removeRuleStopsEnrichment() {
        enricher.addRule(new EnrichmentRule("tag",
                e -> true, "source", "logpulse"));
        enricher.removeRule("tag");

        LogEntry e = entry("svc", LogLevel.DEBUG);
        enricher.enrich(e);

        assertNull(e.getMetadata("source"));
    }

    @Test
    void enrichAllProcessesEveryEntry() {
        enricher.addRule(new EnrichmentRule("stamp",
                e -> true, "processed", "true"));

        List<LogEntry> entries = List.of(
                entry("a", LogLevel.INFO),
                entry("b", LogLevel.WARN),
                entry("c", LogLevel.ERROR));

        enricher.enrichAll(entries);

        entries.forEach(e -> assertEquals("true", e.getMetadata("processed")));
    }

    @Test
    void getRuleCountReflectsAddAndRemove() {
        assertEquals(0, enricher.getRuleCount());
        enricher.addRule(new EnrichmentRule("r1", e -> true, "k", "v"));
        enricher.addRule(new EnrichmentRule("r2", e -> true, "k2", "v2"));
        assertEquals(2, enricher.getRuleCount());
        enricher.removeRule("r1");
        assertEquals(1, enricher.getRuleCount());
    }
}
