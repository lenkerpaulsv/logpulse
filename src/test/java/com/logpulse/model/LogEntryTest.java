package com.logpulse.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LogEntryTest {

    private static final Instant NOW = Instant.parse("2024-06-01T12:00:00Z");

    @Test
    void constructsValidEntryWithFields() {
        Map<String, String> fields = new HashMap<>();
        fields.put("request_id", "abc-123");
        fields.put("user_id", "42");

        LogEntry entry = new LogEntry(NOW, LogEntry.Level.INFO, "auth-service", "Login successful", fields);

        assertEquals(NOW, entry.getTimestamp());
        assertEquals(LogEntry.Level.INFO, entry.getLevel());
        assertEquals("auth-service", entry.getService());
        assertEquals("Login successful", entry.getMessage());
        assertEquals("abc-123", entry.getFields().get("request_id"));
        assertEquals("42", entry.getFields().get("user_id"));
    }

    @Test
    void constructsValidEntryWithNullFields() {
        LogEntry entry = new LogEntry(NOW, LogEntry.Level.ERROR, "payment-service", "Timeout", null);
        assertNotNull(entry.getFields());
        assertTrue(entry.getFields().isEmpty());
    }

    @Test
    void fieldsMapIsImmutable() {
        Map<String, String> fields = new HashMap<>();
        fields.put("k", "v");
        LogEntry entry = new LogEntry(NOW, LogEntry.Level.WARN, "gateway", "Slow response", fields);

        assertThrows(UnsupportedOperationException.class, () -> entry.getFields().put("x", "y"));
    }

    @Test
    void fieldsMapIsDefensiveCopy() {
        Map<String, String> fields = new HashMap<>();
        fields.put("k", "v");
        LogEntry entry = new LogEntry(NOW, LogEntry.Level.INFO, "svc", "msg", fields);

        // Mutating the original map after construction should not affect the entry
        fields.put("k", "modified");
        assertEquals("v", entry.getFields().get("k"));
    }

    @Test
    void throwsOnNullTimestamp() {
        assertThrows(IllegalArgumentException.class,
                () -> new LogEntry(null, LogEntry.Level.INFO, "svc", "msg", null));
    }

    @Test
    void throwsOnBlankService() {
        assertThrows(IllegalArgumentException.class,
                () -> new LogEntry(NOW, LogEntry.Level.INFO, "  ", "msg", null));
    }

    @Test
    void toStringContainsKeyParts() {
        LogEntry entry = new LogEntry(NOW, LogEntry.Level.DEBUG, "order-service", "Processing order", null);
        String str = entry.toString();
        assertTrue(str.contains("order-service"));
        assertTrue(str.contains("DEBUG"));
        assertTrue(str.contains("Processing order"));
    }
}
