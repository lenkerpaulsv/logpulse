package com.logpulse.replay;

import com.logpulse.filter.LogFilter;
import com.logpulse.filter.LogLevel;
import com.logpulse.model.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogReplayerTest {

    private List<LogEntry> snapshot;

    @BeforeEach
    void setUp() {
        snapshot = List.of(
            new LogEntry("svc-a", LogLevel.INFO,  "started",  Instant.ofEpochMilli(1000)),
            new LogEntry("svc-a", LogLevel.WARN,  "slow query", Instant.ofEpochMilli(1100)),
            new LogEntry("svc-b", LogLevel.ERROR, "timeout",  Instant.ofEpochMilli(1200))
        );
    }

    @Test
    void replayDeliversAllEntriesInOrder() throws InterruptedException {
        LogReplayer replayer = new LogReplayer(snapshot, null, 1000.0);
        List<LogEntry> received = new ArrayList<>();
        replayer.replay(received::add);
        assertEquals(3, received.size());
        assertEquals("started",   received.get(0).getMessage());
        assertEquals("slow query", received.get(1).getMessage());
        assertEquals("timeout",   received.get(2).getMessage());
        assertEquals(ReplayStatus.COMPLETED, replayer.getStatus());
    }

    @Test
    void replayAppliesFilterCorrectly() throws InterruptedException {
        LogFilter errorFilter = new LogFilter(LogLevel.ERROR, null);
        LogReplayer replayer = new LogReplayer(snapshot, errorFilter, 1000.0);
        List<LogEntry> received = new ArrayList<>();
        replayer.replay(received::add);
        assertEquals(1, received.size());
        assertEquals(LogLevel.ERROR, received.get(0).getLevel());
    }

    @Test
    void replayOnEmptySnapshotCompletesImmediately() throws InterruptedException {
        LogReplayer replayer = new LogReplayer(List.of());
        List<LogEntry> received = new ArrayList<>();
        replayer.replay(received::add);
        assertTrue(received.isEmpty());
        assertEquals(ReplayStatus.COMPLETED, replayer.getStatus());
    }

    @Test
    void initialStatusIsIdle() {
        LogReplayer replayer = new LogReplayer(snapshot);
        assertEquals(ReplayStatus.IDLE, replayer.getStatus());
    }

    @Test
    void invalidSpeedMultiplierThrows() {
        assertThrows(IllegalArgumentException.class, () -> new LogReplayer(snapshot, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new LogReplayer(snapshot, null, -1));
    }

    @Test
    void nullSnapshotThrows() {
        assertThrows(IllegalArgumentException.class, () -> new LogReplayer(null));
    }

    @Test
    void replayStatusIsTerminalAfterCompletion() throws InterruptedException {
        LogReplayer replayer = new LogReplayer(snapshot, null, 1000.0);
        replayer.replay(e -> {});
        assertTrue(replayer.getStatus().isTerminal());
        assertFalse(replayer.getStatus().isActive());
    }

    @Test
    void getSnapshotSizeReflectsInput() {
        LogReplayer replayer = new LogReplayer(snapshot);
        assertEquals(3, replayer.getSnapshotSize());
    }
}
