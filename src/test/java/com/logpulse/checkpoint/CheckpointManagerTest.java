package com.logpulse.checkpoint;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CheckpointManagerTest {

    @TempDir
    Path tempDir;

    private CheckpointManager manager;

    @BeforeEach
    void setUp() throws IOException {
        manager = new CheckpointManager(tempDir);
    }

    @Test
    void saveAndLoadCheckpoint() throws IOException {
        manager.saveCheckpoint("service-auth", 2048L);
        LogCheckpoint cp = manager.loadCheckpoint("service-auth");
        assertNotNull(cp);
        assertEquals("service-auth", cp.getSourceId());
        assertEquals(2048L, cp.getByteOffset());
        assertNotNull(cp.getSavedAt());
    }

    @Test
    void loadCheckpointFromDisk() throws IOException {
        manager.saveCheckpoint("service-payments", 4096L);
        // Create a fresh manager pointing to the same dir to simulate restart
        CheckpointManager reloaded = new CheckpointManager(tempDir);
        LogCheckpoint cp = reloaded.loadCheckpoint("service-payments");
        assertNotNull(cp);
        assertEquals(4096L, cp.getByteOffset());
    }

    @Test
    void hasCheckpointReturnsTrueAfterSave() throws IOException {
        assertFalse(manager.hasCheckpoint("service-orders"));
        manager.saveCheckpoint("service-orders", 512L);
        assertTrue(manager.hasCheckpoint("service-orders"));
    }

    @Test
    void deleteCheckpointRemovesEntry() throws IOException {
        manager.saveCheckpoint("service-inventory", 100L);
        assertTrue(manager.hasCheckpoint("service-inventory"));
        manager.deleteCheckpoint("service-inventory");
        assertFalse(manager.hasCheckpoint("service-inventory"));
        assertNull(manager.loadCheckpoint("service-inventory"));
    }

    @Test
    void loadNonExistentCheckpointReturnsNull() throws IOException {
        assertNull(manager.loadCheckpoint("unknown-source"));
    }

    @Test
    void checkpointWithSpecialCharactersInSourceId() throws IOException {
        String sourceId = "/var/log/app/service.log";
        manager.saveCheckpoint(sourceId, 768L);
        LogCheckpoint cp = manager.loadCheckpoint(sourceId);
        assertNotNull(cp);
        assertEquals(768L, cp.getByteOffset());
    }

    @Test
    void logCheckpointRejectsNegativeOffset() {
        assertThrows(IllegalArgumentException.class,
                () -> new LogCheckpoint("svc", -1L, Instant.now()));
    }

    @Test
    void logCheckpointRejectsBlankSourceId() {
        assertThrows(IllegalArgumentException.class,
                () -> new LogCheckpoint("  ", 0L, Instant.now()));
    }

    @Test
    void overwritingCheckpointUpdatesOffset() throws IOException {
        manager.saveCheckpoint("service-cache", 100L);
        manager.saveCheckpoint("service-cache", 9999L);
        LogCheckpoint cp = manager.loadCheckpoint("service-cache");
        assertEquals(9999L, cp.getByteOffset());
    }
}
