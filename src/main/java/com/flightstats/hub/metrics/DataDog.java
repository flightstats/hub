package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Singleton;
import com.timgroup.statsd.Event;
import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;

import static com.flightstats.hub.app.HubServices.TYPE.AFTER_HEALTHY_START;

@Singleton
public class DataDog {

    public final static StatsDClient statsd = HubProperties.getProperty("data_dog.enable", false) ?
            new NonBlockingStatsDClient("hub", "localhost", 8125)
            : new NoOpStatsDClient();

    static {
        HubServices.register(new DataDogService(), AFTER_HEALTHY_START);
    }

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
            //do nothing
        }
    }
}

