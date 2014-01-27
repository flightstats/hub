package com.flightstats.datahub.replication;

import com.flightstats.datahub.migration.ChannelUtils;
import com.flightstats.datahub.model.Content;
import com.flightstats.datahub.service.ChannelHypermediaLinkBuilder;
import com.flightstats.datahub.util.RuntimeInterruptedException;
import com.google.common.base.Optional;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SequenceIterator uses a WebSocket to keep up with the latest sequence.
 * It is designed to skip over missing sequences, should they occur.
 * SequenceIterator is not thread safe, and should only be used from a single thread.
 *
 */
@WebSocket(maxMessageSize = 1024)
public class SequenceIterator implements Iterator<Content> {

    //todo - gfm - 1/26/14 - look at using Java WebSocket client

    private final static Logger logger = LoggerFactory.getLogger(SequenceIterator.class);
    private final ChannelUtils channelUtils;
    private final String channelUrl;
    private final Object lock = new Object();

    private AtomicLong latest;
    private long current;
    private AtomicBoolean shouldExit = new AtomicBoolean(false);
    private WebSocketClient client;

    public SequenceIterator(long startSequence, ChannelUtils channelUtils, String channelUrl) {
        this.current = startSequence;
        this.channelUtils = channelUtils;
        this.channelUrl = channelUrl;
        startSocket();
    }

    @Override
    public boolean hasNext() {
        while (!shouldExit.get()) {
            if (current < latest.get()) {
                current++;
                return true;
            }
            synchronized (lock) {
                try {
                    lock.wait(TimeUnit.MINUTES.toMillis(5));
                } catch (InterruptedException e) {
                    throw new RuntimeInterruptedException(e);
                }
            }
        }
        try {
            client.stop();
        } catch (Exception e) {
            logger.warn("issue trying to stop ", e);
        }
        return false;
    }

    @Override
    public Content next() {
        //todo - gfm - 1/25/14 - still wondering about this logic
        Optional<Content> optional = channelUtils.getContent(channelUrl, current);
        while (!optional.isPresent()) {
            //todo - gfm - 1/25/14 - seems like this missing records should be logged somewhere, perhaps to a missing records channel
            logger.warn("unable to get record " + channelUrl + " " + current);
            current++;
            optional = channelUtils.getContent(channelUrl, current);
        }
        return optional.get();
    }

    private void startSocket() {
        Optional<Long> latestSequence = channelUtils.getLatestSequence(channelUrl);
        if (!latestSequence.isPresent()) {
            logger.warn("unable to get latest for channel " + channelUrl);
            return;
        }
        URI wsUri = ChannelHypermediaLinkBuilder.buildWsLinkFor(URI.create(channelUrl));
        latest = new AtomicLong(latestSequence.get());
        startWebSocket(wsUri);
    }

    private void startWebSocket(URI channelWsUrl) {
        try {
            client = new WebSocketClient();
            client.start();
            client.connect(this, channelWsUrl);
        } catch (Exception e) {
            logger.warn("unable to start ", e);
            throw new RuntimeException("unable to start socket", e);
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("don't call this");
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        logger.info("Connection closed: " + statusCode + " " + reason);
        exit();
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        logger.info("connected");
    }

    @OnWebSocketMessage
    public void onMessage(String msg) {
        //todo - gfm - 1/26/14 - does this need to do anything with errors?
        logger.info("message {}", msg);
        long sequence = Long.parseLong(StringUtils.substringAfterLast(msg, "/"));
        //todo - gfm - 1/25/14 - presumes this is called in a single threaded event loop
        if (sequence > latest.get()) {
            latest.set(sequence);
        }
        signal();
    }

    @OnWebSocketError
    public void onError(Session session, Throwable throwable) {
        logger.warn("unexpected WS error " + channelUrl, throwable);
        exit();
    }

    private void exit() {
        shouldExit.set(true);
        signal();
    }

    private void signal() {
        synchronized (lock) {
            lock.notify();
        }
    }


}
