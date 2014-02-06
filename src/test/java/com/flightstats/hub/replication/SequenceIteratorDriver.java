package com.flightstats.hub.replication;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.flightstats.hub.app.config.GuiceContext;
import com.flightstats.hub.model.Content;
import com.sun.jersey.api.client.Client;
import org.eclipse.jetty.websocket.jsr356.ClientContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 *
 */
public class SequenceIteratorDriver {
    private final static Logger logger = LoggerFactory.getLogger(SequenceIteratorDriver.class);

    public static void main(String[] args) throws Exception {
        Client noRedirects = GuiceContext.HubCommonModule.buildJerseyClientNoRedirects();
        Client follows = GuiceContext.HubCommonModule.buildJerseyClient();

        ChannelUtils channelUtils = new ChannelUtils(noRedirects, follows);
        ClientContainer container = new ClientContainer();
        container.start();
        MetricRegistry metricRegistry = new MetricRegistry();
        ConsoleReporter reporter = ConsoleReporter.forRegistry(metricRegistry).build();
        reporter.start(1, TimeUnit.SECONDS);
        Channel testy10 = new Channel("testy10", "http://hub.svc.dev/channel/testy10");
        SequenceIterator iterator = new SequenceIterator(700457, channelUtils, testy10, container, metricRegistry);

        while (iterator.hasNext()) {
            Content next = iterator.next();
            //logger.info("next " + next.getContentKey().get().keyToString());
        }
        logger.info("exited ");
    }



}
