package com.flightstats.datahub.replication;

import com.flightstats.datahub.model.Content;
import com.flightstats.datahub.service.ChannelLinkBuilder;
import com.flightstats.datahub.util.RuntimeInterruptedException;
import com.google.common.base.Optional;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
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
@ClientEndpoint()
public class SequenceIterator implements Iterator<Content> {

    private final static Logger logger = LoggerFactory.getLogger(SequenceIterator.class);
    private final ChannelUtils channelUtils;
    private final WebSocketContainer container;
    private final String channelUrl;
    private final Object lock = new Object();

    private AtomicLong latest;
    private long current;
    private AtomicBoolean shouldExit = new AtomicBoolean(false);

    public SequenceIterator(long startSequence, ChannelUtils channelUtils, String channelUrl, WebSocketContainer container) {
        this.current = startSequence;
        this.channelUtils = channelUtils;
        this.container = container;
        if (channelUrl.endsWith("/")) {
            channelUrl = channelUrl.substring(0, channelUrl.length() - 1);
        }
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
        return false;
    }

    @Override
    public Content next() {
        Optional<Content> optional = channelUtils.getContent(channelUrl, current);
        while (!optional.isPresent()) {
            //todo - gfm - 1/25/14 - seems like this missing records should be logged somewhere, perhaps to a missing records channel
            logger.warn("unable to get content " + channelUrl + current);
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
        URI wsUri = ChannelLinkBuilder.buildWsLinkFor(URI.create(channelUrl));
        latest = new AtomicLong(latestSequence.get());
        startWebSocket(wsUri);
    }

    private void startWebSocket(URI channelWsUrl) {
        try {
            container.connectToServer(this, channelWsUrl);
        } catch (Exception e) {
            logger.warn("unable to start ", e);
            throw new RuntimeException("unable to start socket", e);
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("don't call this");
    }

    @OnClose
    public void onClose(CloseReason reason) {
        logger.info("Connection closed: " + reason);
        exit();
    }

    @OnOpen
    public void onOpen() {
        logger.info("connected " + channelUrl);
    }

    @OnMessage
    public void onMessage(String message) {
        //todo - gfm - 1/26/14 - does this need to do anything with parsing errors?
        logger.info("message {}", message);
        long sequence = Long.parseLong(StringUtils.substringAfterLast(message, "/"));
        if (sequence > latest.get()) {
            latest.set(sequence);
        }
        signal();
    }

    @OnError
    public void onError(Throwable throwable) {
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
