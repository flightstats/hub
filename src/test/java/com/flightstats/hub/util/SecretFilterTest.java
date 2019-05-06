package com.flightstats.hub.util;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretFilterTest {
    private final static SecretFilter secretFilter = new SecretFilter();
    @Test
    void testRedactionFilter_sensitivePatterns_redacted() {
        assertEquals("[REDACTED]", secretFilter.redact("app_key", "test_secret"));
        assertEquals("[REDACTED]", secretFilter.redact("api_key", "test_secret"));
        assertEquals("[REDACTED]", secretFilter.redact("password", "test_secret"));
        assertEquals("[REDACTED]", secretFilter.redact("arbitrary.string.password", "test_secret"));
        assertEquals("[REDACTED]", secretFilter.redact("arbitrary.*.api_key", "test_secret"));
        assertEquals("[REDACTED]", secretFilter.redact("arbitrary.|||.app_key", "test_secret"));
    }

    @Test
    void testRedactionFilter_nonSensitivePatterns_identity() {
        assertTrue(Stream.of("nonsensitive", "arbitrary", "strings", "arbitrary.|||.pattern", "arbitrary.|||.pass")
                .allMatch((str) -> secretFilter.redact(str, "identity").equals("identity")));
    }
}
