package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.google.common.util.concurrent.AbstractIdleService;
import com.timgroup.statsd.Event;
import com.timgroup.statsd.Event.Builder;
import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import lombok.Getter;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.flightstats.hub.app.HubServices.TYPE.AFTER_HEALTHY_START;

@Singleton
public class DataDog {

    @Getter
    public final StatsDClient statsDClient;

    @Inject
    DataDog(HubProperties hubProperties) {
        boolean isEnabled = hubProperties.getProperty("data_dog.enable", false);
        if (isEnabled) {
            this.statsDClient = new NonBlockingStatsDClient("hub", "localhost", 8125);
        } else {
            this.statsDClient = new NoOpStatsDClient();
        }

        HubServices.register(new DataDogService(), AFTER_HEALTHY_START);
    }


    public Builder getEventBuilder() {
        return Event.builder()
                .withHostname(HubHost.getLocalName())
                .withPriority(Event.Priority.NORMAL)
                .withAlertType(Event.AlertType.WARNING);
    }

    private class DataDogService extends AbstractIdleService {

        @Override
        protected void startUp() {
            Event event = getEventBuilder()
                    .withTitle("Hub Restart Started")
                    .withText("started")
                    .build();
            getStatsDClient().recordEvent(event, "restart", "started");
        }

        @Override
        protected void shutDown() {
            //do nothing
        }
    }
}

