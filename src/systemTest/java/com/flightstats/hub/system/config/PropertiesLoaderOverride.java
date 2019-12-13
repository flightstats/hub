package com.flightstats.hub.system.config;

import com.flightstats.hub.cluster.Cluster;
import com.flightstats.hub.system.resiliency.ClusterRollingRestartTest;
import com.flightstats.hub.system.resiliency.PlaceholderTest;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.Properties;

@Value
@Slf4j
public class PropertiesLoaderOverride {
    private final Properties properties;

    public PropertiesLoaderOverride(Properties properties) {
        this.properties = properties;
    }


    private Properties placeholderTestRegister() {
        properties.setProperty(PropertiesName.HUB_DOCKER_IMAGE, PlaceholderTest.IMAGE);
        return properties;
    }

    private Properties clusterRollingRestartTestRegister() {
        properties.setProperty(PropertiesName.ZK_NODE_COUNT,
                ClusterRollingRestartTest.ZK_NODE_COUNT);
        properties.setProperty(PropertiesName.HUB_NODE_COUNT,
                ClusterRollingRestartTest.HUB_NODE_COUNT);
        properties.setProperty(PropertiesName.S3_VERIFIER_OFFSET,
                ClusterRollingRestartTest.S3_VERIFIER_OFFSET);
        properties.setProperty(PropertiesName.SPOKE_WRITE_MINUTES,
                ClusterRollingRestartTest.SPOKE_WRITE_MINUTES);
        return properties;
    }

    public Properties get(Object test) {
        if (test.getClass().equals(PlaceholderTest.class)) {
            return placeholderTestRegister();
        }
        if (test.getClass().equals(ClusterRollingRestartTest.class)) {
            return clusterRollingRestartTestRegister();
        }
        return properties;
    }

}
