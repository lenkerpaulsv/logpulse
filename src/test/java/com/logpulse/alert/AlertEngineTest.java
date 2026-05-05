package com.logpulse.alert;

import com.logpulse.filter.LogLevel;
import com.logpulse.model.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AlertEngineTest {

    private AlertEngine engine;
    private List<AlertEvent> firedEvents;

    @BeforeEach
    void setUp() {
        engine = new AlertEngine();
        firedEvents = new ArrayList<>();
        engine.addAlertListener(firedEvents::add);
    }

    @Test
    void evaluate_belowThreshold_noAlertFired() {
        engine.registerRule(new AlertRule("r1", "api", LogLevel.ERROR, 3, 60));
        engine.evaluate(buildEntry("api", LogLevel.ERROR, Instant.now()));
        engine.evaluate(buildEntry("api", LogLevel.ERROR, Instant.now()));
        assertTrue(firedEvents.isEmpty());
    }

    @Test
    void evaluate_reachesThreshold_alertFired() {
        engine.registerRule(new AlertRule("r1", "api", LogLevel.ERROR, 3, 60));
        Instant base = Instant.now();
        engine.evaluate(buildEntry("api", LogLevel.ERROR, base));
        engine.evaluate(buildEntry("api", LogLevel.ERROR, base.plusSeconds(1)));
        engine.evaluate(buildEntry("api", LogLevel.ERROR, base.plusSeconds(2)));
        assertEquals(1, firedEvents.size());
        assertEquals("r1", firedEvents.get(0).getRule().getRuleId());
        assertEquals(3, firedEvents.get(0).getCount());
    }

    @Test
    void evaluate_differentService_noAlertFired() {
        engine.registerRule(new AlertRule("r1", "api", LogLevel.ERROR, 2, 60));
        engine.evaluate(buildEntry("auth", LogLevel.ERROR, Instant.now()));
        engine.evaluate(buildEntry("auth", LogLevel.ERROR, Instant.now()));
        assertTrue(firedEvents.isEmpty());
    }

    @Test
    void evaluate_levelBelowMinimum_noAlertFired() {
        engine.registerRule(new AlertRule("r1", "api", LogLevel.ERROR, 2, 60));
        engine.evaluate(buildEntry("api", LogLevel.WARN, Instant.now()));
        engine.evaluate(buildEntry("api", LogLevel.WARN, Instant.now()));
        assertTrue(firedEvents.isEmpty());
    }

    @Test
    void evaluate_nullServiceNameRule_matchesAllServices() {
        engine.registerRule(new AlertRule("r1", null, LogLevel.ERROR, 2, 60));
        Instant base = Instant.now();
        engine.evaluate(buildEntry("svc-a", LogLevel.ERROR, base));
        engine.evaluate(buildEntry("svc-b", LogLevel.ERROR, base.plusSeconds(1)));
        assertEquals(1, firedEvents.size());
    }

    @Test
    void evaluate_windowExpired_alertNotFired() {
        engine.registerRule(new AlertRule("r1", "api", LogLevel.ERROR, 2, 10));
        Instant old = Instant.now().minusSeconds(20);
        engine.evaluate(buildEntry("api", LogLevel.ERROR, old));
        engine.evaluate(buildEntry("api", LogLevel.ERROR, Instant.now()));
        assertTrue(firedEvents.isEmpty());
    }

    private LogEntry buildEntry(String service, LogLevel level, Instant timestamp) {
        return new LogEntry(timestamp, level, service, "test message");
    }
}
