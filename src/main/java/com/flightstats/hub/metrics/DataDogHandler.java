package com.flightstats.hub.metrics;

import com.flightstats.hub.config.properties.DatadogMetricsProperties;
import com.flightstats.hub.config.properties.MetricsProperties;
import com.flightstats.hub.rest.RestClient;
import com.flightstats.hub.util.TimeUtil;
import com.google.inject.Inject;
import com.sun.jersey.api.client.ClientResponse;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.MediaType;

@Slf4j
class DataDogHandler {
    private final DatadogMetricsProperties datadogMetricsProperties;
    private final MetricsProperties metricsProperties;

    @Inject
    DataDogHandler(DatadogMetricsProperties datadogMetricsProperties,
                   MetricsProperties metricsProperties) {
        this.datadogMetricsProperties = datadogMetricsProperties;
        this.metricsProperties = metricsProperties;
    }

    void mute() {
        log.info("Attempting to mute datadog");
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
