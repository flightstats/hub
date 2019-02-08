package com.flightstats.hub.metrics;

import com.flightstats.hub.rest.RestClient;
import com.flightstats.hub.util.TimeUtil;
import com.sun.jersey.api.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;

class DataDogHandler {
    private static final Logger logger = LoggerFactory.getLogger(DataDogHandler.class);
    private MetricsConfig metricsConfig;
    private String datadogUrl = "https://app.datadoghq.com/api/v1/downtime";

    DataDogHandler(
            MetricsConfig metricsConfig) {
        this.metricsConfig = metricsConfig;
    }

    DataDogHandler(
            MetricsConfig metricsConfig,
            String datadogUrl
    ) {
        this.metricsConfig = metricsConfig;
        this.datadogUrl = datadogUrl;
    }

    void mute() {
        logger.info("Attempting to mute datadog");
        String apiKey = metricsConfig.getDataDogAPIKey();
        String appKey = metricsConfig.getDataDogAppKey();
        String name = metricsConfig.getHostTag();

        long fourMinutesInSeconds = 4 * 60;
        long nowMillis = TimeUtil.now().getMillis();
        long fourMinutesFutureInSeconds = nowMillis / 1000 + fourMinutesInSeconds;

        if ("".equals(apiKey) || "".equals(appKey)) {
            logger.warn("datadog api_key or app_key not defined");
            return;
        }
        String dataTemplate = "{ \"message\": \"restarting\", \"scope\": \"name:%s\", \"end\": %d }";
        String urlTemplate = "%s?api_key=%s&application_key=%s";
        try {

            String data = String.format(dataTemplate, name, fourMinutesFutureInSeconds);
            String url = String.format(urlTemplate, datadogUrl, apiKey, appKey);
            logger.info("************** {}", url);
            ClientResponse response = RestClient
                    .defaultClient()
                    .resource(url)
                    .type(MediaType.APPLICATION_JSON)
                    .post(ClientResponse.class, data);

            int status = response.getStatus();
            if (status >= 200 && status <= 299) {
                logger.info("Muted datadog monitoring: " + name + " during restart");
            } else {
                logger.warn("Muting datadog monitoring failed: " + name + " status " + status);
            }
        } catch (Exception e) {
            logger.warn("Muting datadog error ", e);
        }
    }
}
