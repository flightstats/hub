package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.util.Sleeper;
import com.google.common.util.concurrent.AbstractIdleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HostedGraphiteSender {

    private final static Logger logger = LoggerFactory.getLogger(HostedGraphiteSender.class);

    private final BlockingQueue<String> queue = new ArrayBlockingQueue<>(10000);
    private final CallableSender callableSender;
    private final String host;
    private final int port;
    private final String graphitePrefix;

    @Inject
    public HostedGraphiteSender(HubProperties hubProperties) {
        this.callableSender = new CallableSender();
        this.host = hubProperties.getProperty("hosted_graphite.host", "carbon.hostedgraphite.com");
        this.port = hubProperties.getProperty("hosted_graphite.port", 2003);

        String apiKey = hubProperties.getProperty("hosted_graphite.apikey", "XYZ");
        String appName = hubProperties.getProperty("app.name", "hub");
        String appEnvironment = hubProperties.getProperty("app.environment", "dev");
        this.graphitePrefix = String.format("{0}.{1}.{2}", apiKey, appName, appEnvironment);

        HubServices.register(new HostedGraphiteSenderService());
    }

    public void send(String name, Object value) {
        if (name.contains(".test")) {
            return;
        }
        try {
            logger.trace("{} to send {}", name, value);
            queue.add(name + " " + value + " " + System.currentTimeMillis() / 1000 + "\n");
        } catch (Exception e) {
            logger.warn("unable to add graphite metric to queue {}", value);
        }
    }

    private class HostedGraphiteSenderService extends AbstractIdleService {
        @Override
        protected void startUp() {
            if (callableSender == null) {
                return;
            }
            Executors.newSingleThreadExecutor().submit(callableSender);
        }

        @Override
        protected void shutDown() {
        }
    }

    private class CallableSender implements Callable<Object> {
        private DataOutputStream stream;

        void connect() {
            while (true) {
                try {
                    Socket socket = new Socket(host, port);
                    socket.setKeepAlive(true);
                    socket.setSoTimeout(10 * 1000);
                    stream = new DataOutputStream(socket.getOutputStream());
                    logger.info("connected to " + host + " " + port);
                    return;
                } catch (IOException e) {
                    logger.warn("unable to connect to " + host + " " + port, e);
                    Sleeper.sleep(1000);
                }
            }
        }

        @Override
        public Object call() {
            connect();
            while (true) {
                try {
                    String value = queue.poll(10, TimeUnit.SECONDS);
                    if (value != null) {
                        send(graphitePrefix + "." + value);
                    }
                } catch (Exception e) {
                    logger.error("unable to send value to graphite", e);
                }
            }
        }

        void send(String toSend) throws IOException {
            logger.trace("sending value {}", toSend);
            try {
                stream.writeBytes(toSend);
            } catch (Exception e) {
                logger.warn("unable to send " + toSend, e);
                connect();
                stream.writeBytes(toSend);
            }
        }
    }

}
