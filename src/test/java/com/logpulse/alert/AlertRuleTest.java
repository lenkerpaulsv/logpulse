package com.logpulse.alert;

import com.logpulse.filter.LogLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlertRuleTest {

    @Test
    void constructor_validArguments_createsRule() {
        AlertRule rule = new AlertRule("r1", "auth-service", LogLevel.ERROR, 5, 60);
        assertEquals("r1", rule.getRuleId());
        assertEquals("auth-service", rule.getServiceName());
        assertEquals(LogLevel.ERROR, rule.getMinimumLevel());
        assertEquals(5, rule.getThreshold());
        assertEquals(60, rule.getWindowSeconds());
    }

    @Test
    void constructor_nullServiceName_isAllowed() {
        AlertRule rule = new AlertRule("r2", null, LogLevel.WARN, 3, 30);
        assertNull(rule.getServiceName());
    }

    @Test
    void constructor_blankRuleId_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new AlertRule("  ", "svc", LogLevel.ERROR, 5, 60));
    }

    @Test
    void constructor_zeroThreshold_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new AlertRule("r3", "svc", LogLevel.ERROR, 0, 60));
    }

    @Test
    void constructor_negativeWindow_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new AlertRule("r4", "svc", LogLevel.ERROR, 5, -1));
    }

    @Test
    void equals_sameRuleId_returnsTrue() {
        AlertRule r1 = new AlertRule("r1", "svc-a", LogLevel.ERROR, 5, 60);
        AlertRule r2 = new AlertRule("r1", "svc-b", LogLevel.WARN, 10, 30);
        assertEquals(r1, r2);
    }

    @Test
    void toString_containsRuleId() {
        AlertRule rule = new AlertRule("my-rule", "gateway", LogLevel.ERROR, 3, 120);
        assertTrue(rule.toString().contains("my-rule"));
    }
}
