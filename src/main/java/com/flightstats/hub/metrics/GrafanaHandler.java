package com.flightstats.hub.metrics;

import com.flightstats.hub.config.properties.DatadogMetricsProperties;
import com.flightstats.hub.config.properties.GrafanaMetricsProperties;
import com.flightstats.hub.config.properties.MetricsProperties;
import com.flightstats.hub.rest.RestClient;
import com.flightstats.hub.util.TimeUtil;
import com.sun.jersey.api.client.ClientResponse;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

@Slf4j
public class GrafanaHandler {
    private final GrafanaMetricsProperties grafanaMetricsProperties;
    private final MetricsProperties metricsProperties;

    @Inject
    GrafanaHandler(GrafanaMetricsProperties grafanaMetricsProperties,
                   MetricsProperties metricsProperties) {
        this.grafanaMetricsProperties = grafanaMetricsProperties;
        this.metricsProperties = metricsProperties;
    }

    void mute() {
        log.debug("Attempting to mute datadog");
        String datadogUrl = grafanaMetricsProperties.getApiUrl() + "/downtime";
        String apiKey = grafanaMetricsProperties.getApiKey();
        String appKey = grafanaMetricsProperties.getAppKey();
        String name = metricsProperties.getHostTag();

        long fourMinutesInSeconds = 4 * 60;
        long nowMillis = TimeUtil.now().getMillis();
        long fourMinutesFutureInSeconds = nowMillis / 1000 + fourMinutesInSeconds;

        if ("".equals(apiKey) || "".equals(appKey)) {
            log.warn("grafana api_key or app_key not defined");
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
                log.info("Muted grafana monitoring: " + name + " during restart");
            } else {
                log.warn("Muting grafana monitoring failed: " + name + " status " + status);
            }
        } catch (Exception e) {
            log.warn("Muting datadog error ", e);
        }
    }
}