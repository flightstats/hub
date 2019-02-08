package com.flightstats.hub.metrics;

import com.flightstats.hub.rest.RestClient;
import com.flightstats.hub.util.TimeUtil;
import com.sun.jersey.api.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;

public class DataDogClient {
    private static final Logger logger = LoggerFactory.getLogger(DataDogClient.class);
    private MetricsConfig metricsConfig;
    private String datadogURL = "https://app.datadoghq.com/api/v1/downtime";

    public DataDogClient(
            MetricsConfig metricsConfig) {
        this.metricsConfig = metricsConfig;
    }

    public DataDogClient(
            MetricsConfig metricsConfig,
            String datadogURL
    ) {
        this.metricsConfig = metricsConfig;
        this.datadogURL = datadogURL;
    }

    void mute() {
        logger.info("Attempting to mute datadog");
        String api_key = metricsConfig.getDataDogAPIKey();
        String app_key = metricsConfig.getDataDogAppKey();
        String name = metricsConfig.getHostTag();

        long fourMinutesInSeconds = 4 * 60;
        long nowMillis = TimeUtil.now().getMillis();
        long fourMinutesFutureInSeconds = nowMillis / 1000 + fourMinutesInSeconds;

        if ("".equals(api_key) || "".equals(app_key)) {
            logger.warn("datadog api_key or app_key not defined");
            return;
        }
        String template = "{ \"message\": \"restarting\", \"scope\": \"name:%s\", \"end\": %d }";
        try {

            String data = String.format(template, name, fourMinutesFutureInSeconds);
            String url = datadogURL +
                    "?api_key=" +
                    api_key +
                    "&application_key=" +
                    app_key;
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
