package com.flightstats.hub.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

@ServerEndpoint(value = "/channel/{channel}/ws")
public class ChannelWSEndpoint {

    private static final WebSocketService webSocketService = WebSocketService.getInstance();

    private final static Logger logger = LoggerFactory.getLogger(ChannelWSEndpoint.class);

    @OnOpen
    public void onOpen(Session session, @PathParam("channel") String channel) throws IOException {
        webSocketService.createCallback(session, channel);
    }

    @OnError
    public void onError(Session session, Throwable throwable, @PathParam("channel") String channel) {
        logger.warn("error " + channel, throwable);
        webSocketService.close(session);
    }

    @OnClose
    public void onClose(Session session, @PathParam("channel") String channel) {
        logger.info("OnClose {}", channel);
        webSocketService.close(session);
    }
}
