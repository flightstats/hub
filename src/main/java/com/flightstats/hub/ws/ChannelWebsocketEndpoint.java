package com.flightstats.hub.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

@ServerEndpoint(value = "/channel/{channel}/ws")
@Singleton
public class ChannelWebsocketEndpoint {
    private final static Logger logger = LoggerFactory.getLogger(ChannelWebsocketEndpoint.class);

    @OnOpen
    public void onOpen(Session session, @PathParam("channel") String channel) throws IOException {
        logger.info("onOpen {}", channel);
        session.getBasicRemote().sendText("onOpen");

    }

    @OnMessage
    public String echo(String message, @PathParam("channel") String channel) {
        logger.info("OnMessage {} {}", channel, message);
        return message + " (from your server)";
    }

    @OnError
    public void onError(Throwable throwable, @PathParam("channel") String channel) {
        logger.info("onError " + channel, throwable);
    }

    @OnClose
    public void onClose(Session session, @PathParam("channel") String channel) {
        logger.info("OnClose {}", channel);
    }
}
