package com.logpulse.export;

import com.logpulse.model.LogEntry;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exports aggregated log entries to various output formats.
 */
public class LogExporter {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final ExportFormat format;

    public LogExporter(ExportFormat format) {
        if (format == null) {
            throw new IllegalArgumentException("Export format must not be null");
        }
        this.format = format;
    }

    public void export(List<LogEntry> entries, Writer writer) throws IOException {
        if (entries == null || writer == null) {
            throw new IllegalArgumentException("Entries and writer must not be null");
        }
        try (BufferedWriter buffered = new BufferedWriter(writer)) {
            switch (format) {
                case CSV:
                    exportCsv(entries, buffered);
                    break;
                case JSON:
                    exportJson(entries, buffered);
                    break;
                case PLAIN:
                default:
                    exportPlain(entries, buffered);
                    break;
            }
        }
    }

    private void exportCsv(List<LogEntry> entries, BufferedWriter writer) throws IOException {
        writer.write("timestamp,service,level,message");
        writer.newLine();
        for (LogEntry entry : entries) {
            writer.write(String.format("%s,%s,%s,\"%s\"",
                    entry.getTimestamp().format(FORMATTER),
                    escapeCsv(entry.getService()),
                    entry.getLevel().name(),
                    escapeCsv(entry.getMessage())));
            writer.newLine();
        }
    }

    private void exportJson(List<LogEntry> entries, BufferedWriter writer) throws IOException {
        writer.write("[");
        writer.newLine();
        for (int i = 0; i < entries.size(); i++) {
            LogEntry entry = entries.get(i);
            writer.write(String.format(
                    "  {\"timestamp\":\"%s\",\"service\":\"%s\",\"level\":\"%s\",\"message\":\"%s\"}",
                    entry.getTimestamp().format(FORMATTER),
                    escapeJson(entry.getService()),
                    entry.getLevel().name(),
                    escapeJson(entry.getMessage())));
            if (i < entries.size() - 1) writer.write(",");
            writer.newLine();
        }
        writer.write("]");
    }

    private void exportPlain(List<LogEntry> entries, BufferedWriter writer) throws IOException {
        for (LogEntry entry : entries) {
            writer.write(String.format("[%s] [%s] [%s] %s",
                    entry.getTimestamp().format(FORMATTER),
                    entry.getLevel().name(),
                    entry.getService(),
                    entry.getMessage()));
            writer.newLine();
        }
    }

    private String escapeCsv(String value) {
        return value == null ? "" : value.replace("\"", "\"\"");
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public ExportFormat getFormat() {
        return format;
    }
}
