package com.flightstats.hub.dao.aws.s3Verifier;

public enum VerifierMetrics {
    MISSING_ITEM("s3.verifier.missing"),
    FAILED("s3.verifier.failed"),
    TIMEOUT("s3.verifier.timeout"),
    PARTIAL_UPDATE("s3.verifier.partial_update"),
    EXCESSIVE_CHANNEL_VOLUME("s3.verifier.excessive_channel_volume");

    private final String name;

    VerifierMetrics(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
