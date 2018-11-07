package com.flightstats.hub.app;

import com.flightstats.hub.filter.MetricsRequestFilter;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerCollection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

class HttpAndWSHandler extends HandlerCollection {

    private Handler httpHandler;
    private Handler wsHandler;

    void addHttpHandler(Handler httpHandler) {
        this.httpHandler = httpHandler;
        addHandler(httpHandler);
    }

    void addWSHandler(Handler wsHandler) {
        this.wsHandler = wsHandler;
        addHandler(wsHandler);
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        if (isStarted()) {
            if (baseRequest.getHttpFields().contains("Upgrade", "websocket")) {
                wsHandler.handle(target, baseRequest, request, response);
            } else {
                httpHandler.handle(target, baseRequest, request, response);
                MetricsRequestFilter.finalStats();
            }
        }
    }
}
