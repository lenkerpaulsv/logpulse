package com.logpulse.routing;

import com.logpulse.filter.LogLevel;
import com.logpulse.model.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogRouterTest {

    private LogRouter router;

    @BeforeEach
    void setUp() {
        router = new LogRouter();
    }

    private LogEntry entry(String service, LogLevel level) {
        return new LogEntry(service, level, "test message", Instant.now());
    }

    @Test
    void routesToMatchingService() {
        List<LogEntry> received = new ArrayList<>();
        router.register("auth-service", LogLevel.INFO, received::add);

        router.route(entry("auth-service", LogLevel.INFO));
        router.route(entry("order-service", LogLevel.INFO));

        assertEquals(1, received.size());
        assertEquals("auth-service", received.get(0).getService());
    }

    @Test
    void filtersEntriesBelowMinLevel() {
        List<LogEntry> received = new ArrayList<>();
        router.register("auth-service", LogLevel.WARN, received::add);

        router.route(entry("auth-service", LogLevel.DEBUG));
        router.route(entry("auth-service", LogLevel.INFO));
        router.route(entry("auth-service", LogLevel.WARN));
        router.route(entry("auth-service", LogLevel.ERROR));

        assertEquals(2, received.size());
    }

    @Test
    void wildcardReceivesAllServices() {
        List<LogEntry> received = new ArrayList<>();
        router.register("*", LogLevel.ERROR, received::add);

        router.route(entry("auth-service", LogLevel.ERROR));
        router.route(entry("order-service", LogLevel.ERROR));
        router.route(entry("payment-service", LogLevel.WARN));

        assertEquals(2, received.size());
    }

    @Test
    void multipleConsumersForSameService() {
        List<LogEntry> first = new ArrayList<>();
        List<LogEntry> second = new ArrayList<>();
        router.register("auth-service", LogLevel.DEBUG, first::add);
        router.register("auth-service", LogLevel.ERROR, second::add);

        router.route(entry("auth-service", LogLevel.ERROR));

        assertEquals(1, first.size());
        assertEquals(1, second.size());
    }

    @Test
    void deregisterRemovesRoutes() {
        List<LogEntry> received = new ArrayList<>();
        router.register("auth-service", LogLevel.DEBUG, received::add);
        router.deregister("auth-service");

        router.route(entry("auth-service", LogLevel.ERROR));

        assertTrue(received.isEmpty());
    }

    @Test
    void routeCountReflectsRegistrations() {
        router.register("auth-service", LogLevel.INFO, e -> {});
        router.register("auth-service", LogLevel.WARN, e -> {});
        router.register("order-service", LogLevel.ERROR, e -> {});

        assertEquals(3, router.routeCount());
    }

    @Test
    void nullEntryDoesNotThrow() {
        assertDoesNotThrow(() -> router.route(null));
    }

    @Test
    void invalidRegistrationThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> router.register("", LogLevel.INFO, e -> {}));
        assertThrows(IllegalArgumentException.class,
                () -> router.register("svc", null, e -> {}));
        assertThrows(IllegalArgumentException.class,
                () -> router.register("svc", LogLevel.INFO, null));
    }
}
