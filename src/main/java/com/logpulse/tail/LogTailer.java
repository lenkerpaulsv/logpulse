package com.logpulse.tail;

import com.logpulse.model.LogEntry;
import com.logpulse.filter.LogFilter;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Tails a log file in real time, parsing new lines as they are appended.
 * Notifies registered listeners with parsed LogEntry objects that pass the
 * optional LogFilter.
 */
public class LogTailer implements Closeable {

    private final Path filePath;
    private final LogFilter filter;
    private final List<Consumer<LogEntry>> listeners = new ArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread tailerThread;

    public LogTailer(Path filePath, LogFilter filter) {
        if (filePath == null) throw new IllegalArgumentException("filePath must not be null");
        this.filePath = filePath;
        this.filter = filter;
    }

    public LogTailer(Path filePath) {
        this(filePath, null);
    }

    public void addListener(Consumer<LogEntry> listener) {
        if (listener == null) throw new IllegalArgumentException("listener must not be null");
        listeners.add(listener);
    }

    public void start() throws IOException {
        if (running.getAndSet(true)) {
            throw new IllegalStateException("LogTailer is already running");
        }
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("Log file not found: " + filePath);
        }
        tailerThread = new Thread(this::tailLoop, "logpulse-tailer-" + filePath.getFileName());
        tailerThread.setDaemon(true);
        tailerThread.start();
    }

    public void stop() {
        running.set(false);
        if (tailerThread != null) {
            tailerThread.interrupt();
        }
    }

    @Override
    public void close() {
        stop();
    }

    public boolean isRunning() {
        return running.get();
    }

    private void tailLoop() {
        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
            raf.seek(raf.length()); // start at end of file
            while (running.get()) {
                String line = raf.readLine();
                if (line == null) {
                    Thread.sleep(200);
                    continue;
                }
                LogEntry entry = LogEntry.parse(line);
                if (entry == null) continue;
                if (filter != null && !filter.matches(entry)) continue;
                for (Consumer<LogEntry> listener : listeners) {
                    listener.accept(entry);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            running.set(false);
            throw new UncheckedIOException("Error tailing file: " + filePath, e);
        }
    }
}
