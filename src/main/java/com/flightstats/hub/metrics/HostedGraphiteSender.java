package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.util.Sleeper;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.*;

public class HostedGraphiteSender implements MetricsSender {
    private final static Logger logger = LoggerFactory.getLogger(HostedGraphiteSender.class);

    private final BlockingQueue<String> queue = new ArrayBlockingQueue<>(10000);
    private final String host;
    private final int port;
    private final String graphitePrefix;
    private CallableSender callableSender;

    @Inject
    public HostedGraphiteSender()
            throws IOException {
        this.host = HubProperties.getProperty("hosted_graphite.host", "carbon.hostedgraphite.com");
        this.port = HubProperties.getProperty("hosted_graphite.port", 2003);

        this.graphitePrefix = HubProperties.getProperty("hosted_graphite.apikey", "XYZ")
                + "." + HubProperties.getProperty("app.name", "hub")
                + "." + HubProperties.getProperty("app.environment", "dev");
        callableSender = new CallableSender();
        HubServices.register(new HostedGraphiteSenderService(), HubServices.TYPE.PRE_START);
    }

    @Override
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
        protected void startUp() throws Exception {
            if (callableSender == null) {
                return;
            }
            callableSender.connect();
            Executors.newSingleThreadExecutor().submit(callableSender);
        }

        @Override
        protected void shutDown() throws Exception {
        }
    }

    class CallableSender implements Callable<Object> {
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
        public Object call() throws Exception {
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
