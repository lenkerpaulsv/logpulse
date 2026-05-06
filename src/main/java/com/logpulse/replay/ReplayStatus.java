package com.logpulse.replay;

/**
 * Represents the lifecycle state of a {@link LogReplayer} session.
 */
public enum ReplayStatus {

    /** Replayer has been created but replay has not started. */
    IDLE,

    /** Replay is currently in progress. */
    RUNNING,

    /** Replay finished processing all entries in the snapshot. */
    COMPLETED,

    /** Replay was manually stopped before completing. */
    STOPPED;

    public boolean isTerminal() {
        return this == COMPLETED || this == STOPPED;
    }

    public boolean isActive() {
        return this == RUNNING;
    }
}
