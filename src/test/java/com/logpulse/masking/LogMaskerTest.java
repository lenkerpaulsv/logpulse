package com.logpulse.masking;

import com.logpulse.filter.LogLevel;
import com.logpulse.model.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogMaskerTest {

    private LogMasker masker;
    private MaskingRule passwordRule;
    private MaskingRule tokenRule;

    @BeforeEach
    void setUp() {
        masker = new LogMasker();
        passwordRule = new MaskingRule("password", "password=[^\\s&]+", "password=[MASKED]");
        tokenRule = new MaskingRule("bearer-token", "Bearer\\s+[A-Za-z0-9._\\-]+", "Bearer [MASKED]");
    }

    @Test
    void maskingRuleAppliesReplacementToMatchingInput() {
        String input = "User login with password=secret123 failed";
        String result = passwordRule.apply(input);
        assertEquals("User login with password=[MASKED] failed", result);
    }

    @Test
    void maskingRuleReturnsInputUnchangedWhenNoMatch() {
        String input = "Normal log message with no sensitive data";
        assertEquals(input, passwordRule.apply(input));
    }

    @Test
    void maskingRuleHandlesNullInput() {
        assertNull(passwordRule.apply(null));
    }

    @Test
    void maskingRuleThrowsOnBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new MaskingRule("", "regex", "[X]"));
    }

    @Test
    void maskingRuleThrowsOnNullRegex() {
        assertThrows(IllegalArgumentException.class, () -> new MaskingRule("rule", null, "[X]"));
    }

    @Test
    void maskAppliesAllRulesToLogEntryMessage() {
        masker.addRule(passwordRule);
        masker.addRule(tokenRule);

        LogEntry entry = new LogEntry(
                Instant.now(), LogLevel.INFO, "auth-service",
                "Auth request with password=hunter2 and Bearer eyJhbGciOiJIUzI1NiJ9",
                "trace-001"
        );

        LogEntry masked = masker.mask(entry);
        assertTrue(masked.getMessage().contains("password=[MASKED]"));
        assertTrue(masked.getMessage().contains("Bearer [MASKED]"));
        assertFalse(masked.getMessage().contains("hunter2"));
    }

    @Test
    void maskPreservesOtherLogEntryFields() {
        masker.addRule(passwordRule);
        Instant now = Instant.now();
        LogEntry entry = new LogEntry(now, LogLevel.WARN, "svc", "password=abc", "t-99");
        LogEntry masked = masker.mask(entry);

        assertEquals(now, masked.getTimestamp());
        assertEquals(LogLevel.WARN, masked.getLevel());
        assertEquals("svc", masked.getService());
        assertEquals("t-99", masked.getTraceId());
    }

    @Test
    void maskReturnsNullForNullEntry() {
        assertNull(masker.mask(null));
    }

    @Test
    void maskAllReturnsMaskedList() {
        masker.addRule(passwordRule);
        List<LogEntry> entries = List.of(
                new LogEntry(Instant.now(), LogLevel.ERROR, "svc", "password=abc", "t1"),
                new LogEntry(Instant.now(), LogLevel.INFO, "svc", "clean message", "t2")
        );
        List<LogEntry> results = masker.maskAll(entries);
        assertEquals(2, results.size());
        assertTrue(results.get(0).getMessage().contains("[MASKED]"));
        assertEquals("clean message", results.get(1).getMessage());
    }

    @Test
    void maskAllReturnsEmptyListForNull() {
        assertTrue(masker.maskAll(null).isEmpty());
    }

    @Test
    void getRuleCountReflectsAddedRules() {
        assertEquals(0, masker.getRuleCount());
        masker.addRule(passwordRule);
        masker.addRule(tokenRule);
        assertEquals(2, masker.getRuleCount());
    }
}
