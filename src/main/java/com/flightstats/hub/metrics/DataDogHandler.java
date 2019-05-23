package com.flightstats.hub.metrics;

import com.flightstats.hub.rest.RestClient;
import com.flightstats.hub.util.TimeUtil;
import com.google.inject.Inject;
import com.sun.jersey.api.client.ClientResponse;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.MediaType;

@Slf4j
class DataDogHandler {
    private MetricsConfig metricsConfig;

    @Inject
    DataDogHandler(MetricsConfig metricsConfig) {
        this.metricsConfig = metricsConfig;
    }

    void mute() {
        log.info("Attempting to mute datadog");
        String datadogUrl = metricsConfig.getDatadogApiUrl() + "/downtime";
        String apiKey = metricsConfig.getDataDogAPIKey();
        String appKey = metricsConfig.getDataDogAppKey();
        String name = metricsConfig.getHostTag();

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
