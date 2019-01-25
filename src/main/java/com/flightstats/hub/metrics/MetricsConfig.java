package com.flightstats.hub.metrics;

import com.flightstats.hub.app.*;
import com.google.inject.Singleton;
import lombok.Builder;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Singleton
@Builder
@Value
public class MetricsConfig {
    private final static Logger logger = LoggerFactory.getLogger(MetricsConfig.class);
    String appVersion;
    String clusterTag;
    boolean enabled;
    String env;
    String hostTag;
    String influxdbDatabaseName;
    String influxdbHost;
    String influxdbPass;
    int influxdbPort;
    String influxdbProtocol;
    String influxdbUser;
    int reportingIntervalSeconds;
    String role;
    String team;

    private static String parseAppVersion() {
        try {
            HubVersion hubVersion = HubProvider.getInstance(HubVersion.class);
            return hubVersion.getVersion();
            // catch null exception because HubProvider is not available in unit tests
        } catch (NullPointerException e) {
            logger.info("no app version available using 'local'", e);
            return "local";
        }
    }

    public static class MetricsConfigBuilder {
        public MetricsConfigBuilder buildWithDefaults() {
            return this.appVersion(parseAppVersion())
                    .clusterTag(
                            HubProperties.getProperty("cluster.location", "local") +
                                    "-" +
                                    HubProperties.getProperty("app.environment", "dev")
                    )
                    .env(HubProperties.getProperty("app.environment", "dev"))
                    .enabled(HubProperties.getProperty("metrics.enable", "false").equals("true"))
                    .hostTag(HubHost.getLocalName())
                    .influxdbDatabaseName(HubProperties.getProperty("metrics.influxdb.database.name", "hub_tick"))
                    .influxdbHost(HubProperties.getProperty("metrics.influxdb.host", "localhost"))
                    .influxdbPass(HubProperties.getProperty("metrics.influxdb.database.password", ""))
                    .influxdbPort(HubProperties.getProperty("metrics.influxdb.port", 8086))
                    .influxdbProtocol(HubProperties.getProperty("metrics.influxdb.protocol", "http"))
                    .influxdbUser(HubProperties.getProperty("metrics.influxdb.database.user", ""))
                    .reportingIntervalSeconds(HubProperties.getProperty("metrics.seconds", 15))
                    .role(HubProperties.getProperty("metrics.tags.role", "hub"))
                    .team(HubProperties.getProperty("metrics.tags.team", "development"));
        }
    }
}
