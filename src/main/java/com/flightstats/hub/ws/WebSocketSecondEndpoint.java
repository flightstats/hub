package com.flightstats.hub.ws;

import com.flightstats.hub.model.ContentKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

@ServerEndpoint(value = "/channel/{channel}/{Y}/{M}/{D}/{h}/{m}/{s}/ws")
public class WebSocketSecondEndpoint {

    private final static Logger logger = LoggerFactory.getLogger(WebSocketChannelEndpoint.class);

    private final WebSocketService webSocketService;

    @Inject
    WebSocketSecondEndpoint(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @OnOpen
    public void onOpen(Session session,
                       @PathParam("channel") String channel,
                       @PathParam("Y") int year,
                       @PathParam("M") int month,
                       @PathParam("D") int day,
                       @PathParam("h") int hour,
                       @PathParam("m") int minute,
                       @PathParam("s") int second
    ) throws IOException {
        ContentKey startingKey = new ContentKey(year, month, day, hour, minute, second, 0);
        webSocketService.createCallback(session, channel, startingKey);
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
