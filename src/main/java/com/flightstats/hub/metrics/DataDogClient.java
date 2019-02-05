package com.flightstats.hub.metrics;

import com.flightstats.hub.rest.RestClient;
import com.sun.jersey.api.client.ClientResponse;
import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;

public class DataDogClient extends StatsDBaseClient {
    private final Logger logger = LoggerFactory.getLogger(DataDogClient.class);
    private static final StatsDClient noopClient =  new NoOpStatsDClient();
    private StatsDClient statsDClient;
    private MetricsConfig metricsConfig;

    public DataDogClient(StatsDClient statsDClient, MetricsConfig metricsConfig) {
        super(statsDClient, metricsConfig);
        this.metricsConfig = metricsConfig;
        this.statsDClient = statsDClient;
    }

    StatsDClient getClient() {
        return metricsConfig.isEnabled() ? statsDClient : noopClient;
    }


    public void mute() {
        logger.info("Attempting to mute datadog");
        String api_key = metricsConfig.getDataDogAPIKey();
        String app_key = metricsConfig.getDataDogAppKey();
        String name = metricsConfig.getHostTag();

        long fourMinutesInSeconds = 4 * 60;
        Instant instant =  new Instant();
        long fourMinutesFutureInSeconds = instant.getMillis() / 1000 + fourMinutesInSeconds;

        if( "".equals(api_key) || "".equals(app_key)) {
            logger.warn("datadog api_key or app_key not defined");
            return;
        }
        String template = "{ \"message\": \"restarting\", \"scope\": \"name:%s\", \"end\": %d }";
        try {

            String data = String.format(template, name, fourMinutesFutureInSeconds);
            String url = "https://app.datadoghq.com/api/v1/downtime?api_key=" +
                    api_key +
                    "&application_key=" +
                    app_key;
            ClientResponse response = RestClient.defaultClient()
                    .resource(url)
                    .type(MediaType.APPLICATION_JSON)
                    .post(ClientResponse.class, data);

            int status = response.getStatus();
            if (status >= 200 && status <= 299  ) {
                logger.info("Muted datadog monitoring: " + name + " during restart");
            } else {
                logger.warn("Muting datadog monitoring failed: " + name + " status " + status);
            }
        }    catch(Exception e){
            logger.warn("Muting datadog error ", e);
        }
    }
}
