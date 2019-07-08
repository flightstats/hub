package com.flightstats.hub.metrics;

public enum MetricsType {
    LIFECYCLE_STARTUP("lifecycle.startup", new String[] { "startup" }),
    LIFECYCLE_SHUTDOWN("lifecycle.shutdown", new String[] { "restart", "shutdown" }),
    WEBHOOK_LEADERSHIP("webhook.leader", new String[] {});

    private String eventType;
    private String[] tags;

    MetricsType(String eventType, String[] tags) {
        this.eventType = eventType;
        this.tags = tags;
    }

    public String getEventType() {
        return eventType;
    }

    public String[] getTags() {
        return tags;
    }
}
