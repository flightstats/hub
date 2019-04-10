package com.flightstats.hub.callback;

import com.flightstats.hub.app.GuiceToHK2Adapter;
import com.google.inject.Injector;
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
    public void start(Injector injector) {

        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        JerseyAppConfig resourceConfig = new JerseyAppConfig();
        resourceConfig.register(new GuiceToHK2Adapter(injector));
        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
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

