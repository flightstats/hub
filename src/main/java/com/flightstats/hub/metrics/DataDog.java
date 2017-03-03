package com.flightstats.hub.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.rest.RestClient;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Singleton;
import com.sun.jersey.api.client.ClientResponse;
import com.timgroup.statsd.Event;
import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;

import static com.flightstats.hub.app.HubServices.TYPE.AFTER_HEALTHY_START;

@Singleton
public class DataDog {
    private final static Logger logger = LoggerFactory.getLogger(DataDogMetricsService.class);
    private final static ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);

    static {
        HubServices.register(new DataDogService(), AFTER_HEALTHY_START);
    }

    public final static StatsDClient statsd = HubProperties.getProperty("data_dog.enable", false) ?
            new NonBlockingStatsDClient("hub", "localhost", 8125)
            : new NoOpStatsDClient();


    public static Event.Builder getEventBuilder() {
        return Event.builder()
                .withHostname(HubHost.getLocalName())
                .withPriority(Event.Priority.NORMAL)
                .withAlertType(Event.AlertType.WARNING);
    }

    private static class DataDogService extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            Event event = DataDog.getEventBuilder()
                    .withTitle("Hub Restart Started")
                    .withText("started")
                    .build();
            DataDog.statsd.recordEvent(event, "restart", "started");
        }

        @Override
        protected void shutDown() throws Exception {
            String api_key = HubProperties.getProperty("datadog.api_key", "");
            String app_key = HubProperties.getProperty("datadog.app_key", "");
            String name = HubHost.getLocalName();
            if( "".equals(api_key) || "".equals(app_key)) return;

            String url = "https://app.datadoghq.com/api/v1/downtime?api_key="
                    + api_key + "&application_key=" + app_key;
            ObjectNode root = mapper.createObjectNode();
            root.put("scope", "name:" + name);
            root.put("message", "restarting");
            root.put("end", (new Instant()).getMillis() + (4*60*60));

            ClientResponse response = RestClient.defaultClient().resource(url)
                    .type(MediaType.APPLICATION_JSON)
                    .put(ClientResponse.class, root);
            if(response.getStatus()==200){
                logger.info("Muting datadog monitoring: " + name + " during restart");
            }else{
                logger.warn("Muting datadog monitoring failed: " + name);
            }
        }
    }
}

