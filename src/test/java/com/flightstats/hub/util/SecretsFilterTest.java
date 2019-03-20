package com.flightstats.hub.util;

import org.junit.Test;

import java.util.stream.Stream;

import static org.junit.Assert.*;

public class SecretsFilterTest {
    @Test
    public void testRedactionFilter_sensitivePatterns_redacted() {
        SecretsFilter secretsFilter = new SecretsFilter();
        assertEquals("[REDACTED]", secretsFilter.redactionFilter("app_key", "test_secret"));
        assertEquals("[REDACTED]", secretsFilter.redactionFilter("api_key", "test_secret"));
        assertEquals("[REDACTED]", secretsFilter.redactionFilter("password", "test_secret"));
        assertEquals("[REDACTED]", secretsFilter.redactionFilter("arbitrary.string.password", "test_secret"));
        assertEquals("[REDACTED]", secretsFilter.redactionFilter("arbitrary.*.api_key", "test_secret"));
        assertEquals("[REDACTED]", secretsFilter.redactionFilter("arbitrary.|||.app_key", "test_secret"));
    }

    @Test
    public void testRedactionFilter_nonSensitivePatterns_identity() {
        SecretsFilter secretsFilter = new SecretsFilter();
        assertTrue(Stream.of("nonsensitive", "arbitrary", "strings", "arbitrary.|||.pattern", "arbitrary.|||.pass")
                .allMatch((str) -> secretsFilter.redactionFilter(str, "identity").equals("identity")));
    }
}
