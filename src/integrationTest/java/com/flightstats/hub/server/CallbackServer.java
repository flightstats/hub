package com.flightstats.hub.server;

import lombok.SneakyThrows;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;

import java.net.InetAddress;

public class CallbackServer {

    private static final int JETTY_PORT = 8090;
    private Server server;

    @SneakyThrows
    public void start() {
        this.server = new Server(JETTY_PORT);
        this.server.setHandler(getWebAppContext());
        this.server.start();
    }

    @SneakyThrows
    public void stop() {
        this.server.stop();
    }

    @SneakyThrows
    private String getHostAddress() {
        return InetAddress.getLocalHost().getHostAddress();
    }

    public String getUrl() {
        return "http://" + getHostAddress() + ":" + JETTY_PORT + "/";
    }

    private WebAppContext getWebAppContext() {
        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setResourceBase("/");
        webAppContext.setContextPath("/");
        webAppContext.addServlet(new ServletHolder(new CallbackServlet()), "/callback");
        return webAppContext;
    }
}
