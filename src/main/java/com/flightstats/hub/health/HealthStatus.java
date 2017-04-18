package com.flightstats.hub.health;

public class HealthStatus {
    private final boolean healthy;
    private final String description;

    @java.beans.ConstructorProperties({"healthy", "description"})
    HealthStatus(boolean healthy, String description) {
        this.healthy = healthy;
        this.description = description;
    }

    public static HealthStatusBuilder builder() {
        return new HealthStatusBuilder();
    }

    public boolean isHealthy() {
        return this.healthy;
    }

    public String getDescription() {
        return this.description;
    }

    public String toString() {
        return "com.flightstats.hub.health.HealthStatus(healthy=" + this.isHealthy() + ", description=" + this.getDescription() + ")";
    }

    public static class HealthStatusBuilder {
        private boolean healthy;
        private String description;

        HealthStatusBuilder() {
        }

        public HealthStatus.HealthStatusBuilder healthy(boolean healthy) {
            this.healthy = healthy;
            return this;
        }

        public HealthStatus.HealthStatusBuilder description(String description) {
            this.description = description;
            return this;
        }

        public HealthStatus build() {
            return new HealthStatus(healthy, description);
        }

        public String toString() {
            return "com.flightstats.hub.health.HealthStatus.HealthStatusBuilder(healthy=" + this.healthy + ", description=" + this.description + ")";
        }
    }
}
