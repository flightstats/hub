package com.flightstats.hub.metrics;

import com.flightstats.hub.cluster.Cluster;
import com.flightstats.hub.config.properties.AppProperties;
import com.sun.jersey.api.client.GenericType;
import com.timgroup.statsd.ServiceCheck;

import com.flightstats.hub.config.properties.DatadogMetricsProperties;
import com.flightstats.hub.config.properties.MetricsProperties;
import com.flightstats.hub.rest.RestClient;
import com.flightstats.hub.util.TimeUtil;

import javax.inject.Inject;

import com.sun.jersey.api.client.ClientResponse;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static com.timgroup.statsd.ServiceCheck.Status.OK;
import static com.timgroup.statsd.ServiceCheck.Status.CRITICAL;

@Slf4j
class DataDogHandler {
    private final DatadogMetricsProperties datadogMetricsProperties;
    private final MetricsProperties metricsProperties;
    private final Cluster curatorCluster;
    private final String appUrl;

    @Inject
    DataDogHandler(DatadogMetricsProperties datadogMetricsProperties,
                   MetricsProperties metricsProperties,
                   Cluster curatorCluster,
                   AppProperties appProperties) {
        this.datadogMetricsProperties = datadogMetricsProperties;
        this.metricsProperties = metricsProperties;
        this.curatorCluster = curatorCluster;
        this.appUrl = appProperties.getAppUrl();
    }

    private Boolean checkNode(String node) {
        try {
            String propertiesPath = "/internal/properties";
            ClientResponse response = RestClient
                    .defaultClient()
                    .resource(appUrl + propertiesPath)
                    .type(MediaType.APPLICATION_JSON)
                    .get(ClientResponse.class);
            Map<String, String> body = response.getEntity(new GenericType<Map<String, String>>() {
            });
            String[] servers = body.get("servers").split(",");
            return Arrays.asList(servers).contains(node);
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
    }


    ServiceCheck serviceCheckNodesInCluster() {
        Set<String> nodes = curatorCluster.getAllServers();
        Boolean nodesPresent = nodes.stream()
                .allMatch(this::checkNode);

        ServiceCheck.Status status = nodesPresent ? OK : CRITICAL;


        String SERVICE_CHECK_NAME = "hub.cluster.synced";
        return ServiceCheck
                .builder()
                .withName(SERVICE_CHECK_NAME)
                .withStatus(status)
                .build();
    }

    void mute() {
        log.debug("Attempting to mute datadog");
        String datadogUrl = datadogMetricsProperties.getApiUrl() + "/downtime";
        String apiKey = datadogMetricsProperties.getApiKey();
        String appKey = datadogMetricsProperties.getAppKey();
        String name = metricsProperties.getHostTag();

        long fourMinutesInSeconds = 4 * 60;
        long nowMillis = TimeUtil.now().getMillis();
        long fourMinutesFutureInSeconds = nowMillis / 1000 + fourMinutesInSeconds;

        if ("".equals(apiKey) || "".equals(appKey)) {
            log.warn("datadog api_key or app_key not defined");
            return;
        }
        String dataTemplate = "{ \"message\": \"restarting\", \"scope\": \"name:%s\", \"end\": %d }";
        String urlTemplate = "%s?api_key=%s&application_key=%s";
        try {

            String data = String.format(dataTemplate, name, fourMinutesFutureInSeconds);
            String url = String.format(urlTemplate, datadogUrl, apiKey, appKey);
            ClientResponse response = RestClient
                    .defaultClient()
                    .resource(url)
                    .type(MediaType.APPLICATION_JSON)
                    .post(ClientResponse.class, data);

            int status = response.getStatus();
            if (status >= 200 && status <= 299) {
                log.info("Muted datadog monitoring: " + name + " during restart");
            } else {
                log.warn("Muting datadog monitoring failed: " + name + " status " + status);
            }
        } catch (Exception e) {
            log.warn("Muting datadog error ", e);
        }
    }
}
