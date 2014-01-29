package com.flightstats.datahub.replication;

import com.flightstats.datahub.app.config.GuiceContextListenerFactory;
import com.flightstats.datahub.model.Content;
import com.sun.jersey.api.client.Client;
import org.eclipse.jetty.websocket.jsr356.ClientContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class SequenceIteratorDriver {
    private final static Logger logger = LoggerFactory.getLogger(SequenceIteratorDriver.class);

    public static void main(String[] args) throws Exception {
        Client noRedirects = GuiceContextListenerFactory.DatahubCommonModule.buildJerseyClientNoRedirects();
        Client follows = GuiceContextListenerFactory.DatahubCommonModule.buildJerseyClient();

        ChannelUtils channelUtils = new ChannelUtils(noRedirects, follows);
        //SequenceIterator iterator = new SequenceIterator(122747, channelUtils, "http://hub.svc.dev/channel/positionsAsdi");
        ClientContainer container = new ClientContainer();
        container.start();
        SequenceIterator iterator = new SequenceIterator(276511, channelUtils, "http://hub.svc.dev/channel/testy10", container);

        while (iterator.hasNext()) {
            Content next = iterator.next();
            logger.info("next " + next.getContentKey().get().keyToString());
        }
        logger.info("exited ");
    }



}
