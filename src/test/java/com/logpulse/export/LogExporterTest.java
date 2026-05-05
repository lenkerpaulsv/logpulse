package com.logpulse.export;

import com.logpulse.filter.LogLevel;
import com.logpulse.model.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogExporterTest {

    private List<LogEntry> entries;

    @BeforeEach
    void setUp() {
        LogEntry e1 = new LogEntry(
                LocalDateTime.of(2024, 6, 1, 10, 0, 0),
                "auth-service",
                LogLevel.INFO,
                "User logged in");
        LogEntry e2 = new LogEntry(
                LocalDateTime.of(2024, 6, 1, 10, 1, 30),
                "payment-service",
                LogLevel.ERROR,
                "Payment failed: timeout");
        entries = List.of(e1, e2);
    }

    @Test
    void exportPlainContainsAllFields() throws IOException {
        LogExporter exporter = new LogExporter(ExportFormat.PLAIN);
        StringWriter sw = new StringWriter();
        exporter.export(entries, sw);
        String output = sw.toString();
        assertTrue(output.contains("auth-service"));
        assertTrue(output.contains("INFO"));
        assertTrue(output.contains("User logged in"));
        assertTrue(output.contains("payment-service"));
        assertTrue(output.contains("ERROR"));
    }

    @Test
    void exportCsvHasHeaderAndRows() throws IOException {
        LogExporter exporter = new LogExporter(ExportFormat.CSV);
        StringWriter sw = new StringWriter();
        exporter.export(entries, sw);
        String output = sw.toString();
        assertTrue(output.startsWith("timestamp,service,level,message"));
        assertTrue(output.contains("auth-service"));
        assertTrue(output.contains("payment-service"));
        assertEquals(3, output.lines().count()); // header + 2 rows
    }

    @Test
    void exportJsonIsValidArray() throws IOException {
        LogExporter exporter = new LogExporter(ExportFormat.JSON);
        StringWriter sw = new StringWriter();
        exporter.export(entries, sw);
        String output = sw.toString().trim();
        assertTrue(output.startsWith("["));
        assertTrue(output.endsWith("]"));
        assertTrue(output.contains("\"service\":\"auth-service\""));
        assertTrue(output.contains("\"level\":\"ERROR\""));
    }

    @Test
    void exportFormatFromStringCaseInsensitive() {
        assertEquals(ExportFormat.CSV, ExportFormat.fromString("csv"));
        assertEquals(ExportFormat.JSON, ExportFormat.fromString("JSON"));
        assertEquals(ExportFormat.PLAIN, ExportFormat.fromString("Plain"));
    }

    @Test
    void exportFormatFromStringThrowsOnUnknown() {
        assertThrows(IllegalArgumentException.class, () -> ExportFormat.fromString("xml"));
        assertThrows(IllegalArgumentException.class, () -> ExportFormat.fromString(""));
        assertThrows(IllegalArgumentException.class, () -> ExportFormat.fromString(null));
    }

    @Test
    void constructorThrowsOnNullFormat() {
        assertThrows(IllegalArgumentException.class, () -> new LogExporter(null));
    }

    @Test
    void exportThrowsOnNullArguments() {
        LogExporter exporter = new LogExporter(ExportFormat.PLAIN);
        assertThrows(IllegalArgumentException.class, () -> exporter.export(null, new StringWriter()));
        assertThrows(IllegalArgumentException.class, () -> exporter.export(entries, null));
    }
}
