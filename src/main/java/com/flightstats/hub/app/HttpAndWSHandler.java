package com.flightstats.hub.app;

import com.flightstats.hub.filter.MetricsRequestFilter;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerCollection;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

class HttpAndWSHandler extends HandlerCollection {

    private final Handler httpHandler;
    private final Handler wsHandler;
    private final MetricsRequestFilter metricsRequestFilter;


    @Inject
    HttpAndWSHandler(Handler httpHandler, Handler wsHandler, MetricsRequestFilter metricsRequestFilter) {
        this.httpHandler = httpHandler;
        this.wsHandler = wsHandler;
        this.metricsRequestFilter = metricsRequestFilter;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        if (isStarted()) {
            if (baseRequest.getHttpFields().contains("Upgrade", "websocket")) {
                wsHandler.handle(target, baseRequest, request, response);
            } else {
                httpHandler.handle(target, baseRequest, request, response);
                metricsRequestFilter.finalStats();
            }
        }
    }
}
