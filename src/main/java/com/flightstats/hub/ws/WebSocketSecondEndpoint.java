package com.flightstats.hub.ws;

import com.flightstats.hub.config.binding.WebSocketConfigurator;
import com.flightstats.hub.model.ContentKey;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

@Slf4j
@ServerEndpoint(
        value = "/channel/{channel}/{Y}/{M}/{D}/{h}/{m}/{s}/ws",
        configurator = WebSocketConfigurator.class)
public class WebSocketSecondEndpoint {

    private final WebSocketService webSocketService;

    @Inject
    private WebSocketSecondEndpoint(WebSocketService webSocketService) {
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
                       @PathParam("s") int second) {
        ContentKey startingKey = new ContentKey(year, month, day, hour, minute, second, 0);
        webSocketService.createCallback(session, channel, startingKey);
    }

    @OnError
    public void onError(Session session, Throwable throwable, @PathParam("channel") String channel) {
        log.warn("error {}", channel, throwable);
        webSocketService.close(session);
    }

    @OnClose
    public void onClose(Session session, @PathParam("channel") String channel) {
        log.info("OnClose {}", channel);
        webSocketService.close(session);
    }
}
