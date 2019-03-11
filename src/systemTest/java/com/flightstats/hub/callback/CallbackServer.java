package com.flightstats.hub.callback;

import lombok.SneakyThrows;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

import java.net.InetAddress;

public class CallbackServer {

    private static final int JETTY_PORT = 8090;
    private Server jettyServer;

    @SneakyThrows
    public void start() {

        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        final ServletContainer servletContainer = new ServletContainer(new JerseyAppConfig());
        final ServletHolder servletHolder = new ServletHolder(servletContainer);
        context.addServlet(servletHolder, "/*");
        servletHolder.setInitOrder(0);
        servletHolder.setInitParameter("" +
                        "jersey.config.server.provider.classnames",
                JerseyAppConfig.class.getCanonicalName());

        this.jettyServer = new Server(JETTY_PORT);
        this.jettyServer.setHandler(context);
        this.jettyServer.start();

    }

    @SneakyThrows
    public void stop() {
        this.jettyServer.stop();
    }

    @SneakyThrows
    private String getHostAddress() {
        return InetAddress.getLocalHost().getHostAddress();
    }

    public String getBaseUrl() {
        return "http://" + getHostAddress() + ":" + JETTY_PORT;
    }
}

