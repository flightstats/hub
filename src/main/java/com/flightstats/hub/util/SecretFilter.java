package com.flightstats.hub.util;

import java.util.Arrays;
import java.util.List;

public class SecretFilter {
    private final static String redacted = "[REDACTED]";
    private final static List<String> matchers = Arrays.asList("app_key", "api_key", "password");

    public String redact(String key, String property) {
        boolean shouldRedact = matchers.stream().anyMatch(key::contains);
        return shouldRedact ? redacted : property;
    }
}
