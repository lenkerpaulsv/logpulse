package com.logpulse.correlation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationContextTest {

    @Test
    void constructor_throwsOnBlankId() {
        assertThrows(IllegalArgumentException.class, () -> new CorrelationContext(""));
        assertThrows(IllegalArgumentException.class, () -> new CorrelationContext(null));
    }

    @Test
    void record_incrementsEntryCount() {
        CorrelationContext ctx = new CorrelationContext("abc-123");
        ctx.record("auth-service");
        ctx.record("auth-service");
        assertEquals(2, ctx.getEntryCount());
    }

    @Test
    void record_deduplicatesServiceNames() {
        CorrelationContext ctx = new CorrelationContext("abc-123");
        ctx.record("auth-service");
        ctx.record("auth-service");
        ctx.record("order-service");
        assertEquals(2, ctx.getServiceNames().size());
    }

    @Test
    void spansMultipleServices_falseForSingleService() {
        CorrelationContext ctx = new CorrelationContext("abc-123");
        ctx.record("auth-service");
        assertFalse(ctx.spansMultipleServices());
    }

    @Test
    void spansMultipleServices_trueForTwoServices() {
        CorrelationContext ctx = new CorrelationContext("abc-123");
        ctx.record("auth-service");
        ctx.record("billing-service");
        assertTrue(ctx.spansMultipleServices());
    }

    @Test
    void lastSeen_updatesAfterRecord() throws InterruptedException {
        CorrelationContext ctx = new CorrelationContext("abc-123");
        var before = ctx.getLastSeen();
        Thread.sleep(5);
        ctx.record("svc");
        assertTrue(ctx.getLastSeen().isAfter(before));
    }

    @Test
    void serviceNamesListIsUnmodifiable() {
        CorrelationContext ctx = new CorrelationContext("abc-123");
        ctx.record("svc-a");
        assertThrows(UnsupportedOperationException.class,
                () -> ctx.getServiceNames().add("svc-b"));
    }
}
