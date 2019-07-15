package com.flightstats.hub.kubernetes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.flightstats.hub.system.config.HelmProperties;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class HelmYamlOverride {
    private static final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    private final HelmProperties helmProperties;

    @Inject
    public HelmYamlOverride(HelmProperties helmProperties) {
        this.helmProperties = helmProperties;
    }

    @SneakyThrows
    String getYaml() {
        String yaml = objectMapper.writeValueAsString(buildValueStructure());
        log.info("starting helm cluster install with yaml: {}", yaml);
        return yaml;
    }

    private Map<String, Object> buildClusteredHubValues() {
        Map<String, Object> clusteredHub = new HashMap<>();
        clusteredHub.put("enabled", helmProperties.isHubInstallClustered());
        return clusteredHub;
    }

    private Map<String, Object> buildHubValues() {
        Map<String, Object> hub = new HashMap<>();
        Map<String, Object> hubInner = new HashMap<>();
        hubInner.put("image", helmProperties.getHubDockerImage());
        hubInner.put("clusteredHub", buildClusteredHubValues());

        hub.put("hub", hubInner);
        return hub;
    }

    private Map<String, Object> buildTagValues() {
        Map<String, Object> tags = new HashMap<>();
        tags.put("installHub", helmProperties.isHubInstalledByHelm());
        tags.put("installZookeeper", helmProperties.isZookeeperInstalledByHelm());
        tags.put("installLocalstack", helmProperties.isLocalstackInstalledByHelm());
        tags.put("installCallbackserver", helmProperties.isCallbackServerInstalledByHelm());
        return tags;
    }

    private Map<String, Map<String, Object>> buildValueStructure() {
        Map<String, Map<String, Object>> yamlValues = new HashMap<>();
        yamlValues.put("tags", buildTagValues());
        yamlValues.put("hub", buildHubValues());
        return yamlValues;
    }

}
