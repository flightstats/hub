package com.flightstats.hub.util;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.Builder;
import lombok.Value;

import java.io.IOException;
import java.net.InetSocketAddress;


@Builder
@Value
public class IntegrationServer {
    HttpHandler testHandler;
    String path;
    String bindAddress;
    int bindPort;

    public final HttpServer httpServer() throws IOException {
        HttpServer server = HttpServer.create();
        server.createContext(path, testHandler);
        InetSocketAddress inetSocketAddress = new InetSocketAddress(bindAddress, bindPort);
        server.bind(inetSocketAddress, 0);
        System.out.println("server listening");
        return server;
    }
}
