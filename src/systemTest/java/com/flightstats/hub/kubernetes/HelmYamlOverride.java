package com.flightstats.hub.kubernetes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.flightstats.hub.system.config.HelmProperties;
import com.flightstats.hub.system.config.PropertiesName;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class HelmYamlOverride {
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final HelmProperties helmProperties;

    @Inject
    public HelmYamlOverride(HelmProperties helmProperties) {
        this.helmProperties = helmProperties;
    }

    @SneakyThrows
    String get() {
        String yaml = mapper.writeValueAsString(build());
        log.info("starting helm cluster install with yaml: {}", yaml);
        return yaml;
    }

    private Map<String, Object> buildClusteredHub() {
        Map<String, Object> clusteredHub = new HashMap<>();
        clusteredHub.put(YamlKeys.CLUSTER_HUB_ENABLED, helmProperties.isHubInstallClustered());
        return clusteredHub;
    }

    private Map<String, Object> buildHub() {
        Map<String, Object> hub = new HashMap<>();
        Map<String, Object> hubInner = new HashMap<>();
        Map<String, Object> statefulset = new HashMap<>();
        Map<String, Object> configMap = new HashMap<>();
        Map<String, Object> properties = new HashMap<>();
        properties.put(
                PropertiesName.S3_VERIFIER_OFFSET,
                helmProperties.getS3VerifierOffset()
        );
        properties.put(PropertiesName.SPOKE_WRITE_MINUTES,
                helmProperties.getSpokeWriteMinutes());
        configMap.put(YamlKeys.PROPERTIES, properties);
        hubInner.put(YamlKeys.CONFIG_MAP, configMap);
        hubInner.put(YamlKeys.IMAGE, helmProperties.getHubDockerImage());
        hubInner.put(YamlKeys.CLUSTER_HUB, buildClusteredHub());
        statefulset.put(YamlKeys.REPLICAS, helmProperties.getHubNodeCount());
        hubInner.put(YamlKeys.STATEFULSET, statefulset);
        hub.put(YamlKeys.HUB, hubInner);
        return hub;
    }

    private Map<String, Object> buildZk() {
        Map<String, Object> zookeeper = new HashMap<>();
        Map<String, Object> zookeeperInner = new HashMap<>();
        Map<String, Object> statefulset = new HashMap<>();
        statefulset.put(YamlKeys.REPLICAS, helmProperties.getZkNodeCount());
        zookeeperInner.put(YamlKeys.STATEFULSET, statefulset);
        zookeeper.put(YamlKeys.ZOOKEEPER, zookeeperInner);
        return zookeeper;
    }

    private Map<String, Object> buildTags() {
        Map<String, Object> tags = new HashMap<>();
        tags.put(YamlKeys.INSTALL_HUB, helmProperties.isHubInstalledByHelm());
        tags.put(YamlKeys.INSTALL_ZK, helmProperties.isZookeeperInstalledByHelm());
        tags.put(YamlKeys.INSTALL_LOCALSTACK, helmProperties.isLocalstackInstalledByHelm());
        tags.put(YamlKeys.INSTALL_CB_SERVER, helmProperties.isCallbackServerInstalledByHelm());
        return tags;
    }

    private Map<String, Map<String, Object>> build() {
        Map<String, Map<String, Object>> yamlValues = new HashMap<>();
        yamlValues.put(YamlKeys.TAGS, buildTags());
        yamlValues.put(YamlKeys.HUB, buildHub());
        yamlValues.put(YamlKeys.ZOOKEEPER, buildZk());
        return yamlValues;
    }

}
