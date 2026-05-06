package com.logpulse.replay;

import com.logpulse.model.LogEntry;
import com.logpulse.filter.LogFilter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Replays historical log entries from a snapshot, optionally filtered and
 * delivered at a configurable speed multiplier.
 */
public class LogReplayer {

    private final List<LogEntry> snapshot;
    private final LogFilter filter;
    private double speedMultiplier;
    private ReplayStatus status;

    public LogReplayer(List<LogEntry> snapshot, LogFilter filter, double speedMultiplier) {
        if (snapshot == null) throw new IllegalArgumentException("Snapshot must not be null");
        if (speedMultiplier <= 0) throw new IllegalArgumentException("Speed multiplier must be positive");
        this.snapshot = new ArrayList<>(snapshot);
        this.snapshot.sort(Comparator.comparing(LogEntry::getTimestamp));
        this.filter = filter;
        this.speedMultiplier = speedMultiplier;
        this.status = ReplayStatus.IDLE;
    }

    public LogReplayer(List<LogEntry> snapshot) {
        this(snapshot, null, 1.0);
    }

    public void replay(Consumer<LogEntry> handler) throws InterruptedException {
        if (snapshot.isEmpty()) {
            status = ReplayStatus.COMPLETED;
            return;
        }
        status = ReplayStatus.RUNNING;
        Instant previousTimestamp = null;
        for (LogEntry entry : snapshot) {
            if (status == ReplayStatus.STOPPED) break;
            if (filter != null && !filter.matches(entry)) continue;
            if (previousTimestamp != null) {
                long delayMillis = entry.getTimestamp().toEpochMilli()
                        - previousTimestamp.toEpochMilli();
                long adjustedDelay = (long) (delayMillis / speedMultiplier);
                if (adjustedDelay > 0) Thread.sleep(adjustedDelay);
            }
            handler.accept(entry);
            previousTimestamp = entry.getTimestamp();
        }
        if (status != ReplayStatus.STOPPED) status = ReplayStatus.COMPLETED;
    }

    public void stop() {
        this.status = ReplayStatus.STOPPED;
    }

    public ReplayStatus getStatus() {
        return status;
    }

    public void setSpeedMultiplier(double speedMultiplier) {
        if (speedMultiplier <= 0) throw new IllegalArgumentException("Speed multiplier must be positive");
        this.speedMultiplier = speedMultiplier;
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public int getSnapshotSize() {
        return snapshot.size();
    }
}
