package com.flightstats.hub.ws;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class HubWebSocketServlet extends WebSocketServlet {

    public void configure(WebSocketServletFactory factory) {
        factory.register(WebSocketChannelEndpoint.class);
        factory.register(WebSocketDayEndpoint.class);
        factory.register(WebSocketHourEndpoint.class);
        factory.register(WebSocketMinuteEndpoint.class);
        factory.register(WebSocketSecondEndpoint.class);
        factory.register(WebSocketHashEndpoint.class);
    }

}
