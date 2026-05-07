package com.logpulse.checkpoint;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages read checkpoints for log tailers so that log processing can resume
 * from the last known position after a restart.
 */
public class CheckpointManager {

    private final Path checkpointDir;
    private final Map<String, LogCheckpoint> checkpoints = new ConcurrentHashMap<>();

    public CheckpointManager(Path checkpointDir) throws IOException {
        this.checkpointDir = checkpointDir;
        Files.createDirectories(checkpointDir);
    }

    public void saveCheckpoint(String sourceId, long byteOffset) throws IOException {
        LogCheckpoint cp = new LogCheckpoint(sourceId, byteOffset, Instant.now());
        checkpoints.put(sourceId, cp);
        Path file = checkpointDir.resolve(sanitize(sourceId) + ".cp");
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write(byteOffset + "," + cp.getSavedAt().toEpochMilli());
        }
    }

    public LogCheckpoint loadCheckpoint(String sourceId) throws IOException {
        if (checkpoints.containsKey(sourceId)) {
            return checkpoints.get(sourceId);
        }
        Path file = checkpointDir.resolve(sanitize(sourceId) + ".cp");
        if (!Files.exists(file)) {
            return null;
        }
        String line = Files.readString(file).trim();
        String[] parts = line.split(",");
        long offset = Long.parseLong(parts[0]);
        Instant savedAt = Instant.ofEpochMilli(Long.parseLong(parts[1]));
        LogCheckpoint cp = new LogCheckpoint(sourceId, offset, savedAt);
        checkpoints.put(sourceId, cp);
        return cp;
    }

    public void deleteCheckpoint(String sourceId) throws IOException {
        checkpoints.remove(sourceId);
        Path file = checkpointDir.resolve(sanitize(sourceId) + ".cp");
        Files.deleteIfExists(file);
    }

    public boolean hasCheckpoint(String sourceId) {
        if (checkpoints.containsKey(sourceId)) return true;
        return Files.exists(checkpointDir.resolve(sanitize(sourceId) + ".cp"));
    }

    private String sanitize(String sourceId) {
        return sourceId.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}
