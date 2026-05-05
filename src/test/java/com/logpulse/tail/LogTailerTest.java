package com.logpulse.tail;

import com.logpulse.filter.LogFilter;
import com.logpulse.filter.LogLevel;
import com.logpulse.model.LogEntry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class LogTailerTest {

    @TempDir
    Path tempDir;

    private Path logFile;

    @BeforeEach
    void setUp() throws IOException {
        logFile = tempDir.resolve("test.log");
        Files.createFile(logFile);
    }

    @Test
    void startsAndStopsCleanly() throws Exception {
        LogTailer tailer = new LogTailer(logFile);
        tailer.start();
        assertTrue(tailer.isRunning());
        tailer.stop();
        // allow thread to wind down
        Thread.sleep(300);
        assertFalse(tailer.isRunning());
    }

    @Test
    void throwsWhenFileDoesNotExist() {
        Path missing = tempDir.resolve("missing.log");
        LogTailer tailer = new LogTailer(missing);
        assertThrows(FileNotFoundException.class, tailer::start);
    }

    @Test
    void throwsWhenStartedTwice() throws Exception {
        LogTailer tailer = new LogTailer(logFile);
        tailer.start();
        try {
            assertThrows(IllegalStateException.class, tailer::start);
        } finally {
            tailer.stop();
        }
    }

    @Test
    void notifiesListenerForNewLines() throws Exception {
        List<LogEntry> received = new ArrayList<>();
        LogTailer tailer = new LogTailer(logFile);
        tailer.addListener(received::add);
        tailer.start();

        try (BufferedWriter writer = Files.newBufferedWriter(logFile, StandardOpenOption.APPEND)) {
            writer.write("2024-01-15T10:00:00Z INFO  auth-service User logged in userId=42\n");
            writer.flush();
        }

        // wait up to 2 s for the entry to arrive
        long deadline = System.currentTimeMillis() + 2000;
        while (received.isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }
        tailer.stop();

        assertFalse(received.isEmpty(), "Expected at least one log entry");
        assertEquals("auth-service", received.get(0).getService());
    }

    @Test
    void filterExcludesNonMatchingEntries() throws Exception {
        List<LogEntry> received = new ArrayList<>();
        LogFilter filter = new LogFilter(LogLevel.ERROR, null, null);
        LogTailer tailer = new LogTailer(logFile, filter);
        tailer.addListener(received::add);
        tailer.start();

        try (BufferedWriter writer = Files.newBufferedWriter(logFile, StandardOpenOption.APPEND)) {
            writer.write("2024-01-15T10:00:01Z DEBUG auth-service debug noise\n");
            writer.write("2024-01-15T10:00:02Z ERROR auth-service something failed\n");
            writer.flush();
        }

        Thread.sleep(800);
        tailer.stop();

        assertEquals(1, received.size());
        assertEquals(LogLevel.ERROR, received.get(0).getLevel());
    }
}
