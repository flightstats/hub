package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.util.Sleeper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.*;

public class HostedGraphiteSender {
    private final static Logger logger = LoggerFactory.getLogger(HostedGraphiteSender.class);

    private final BlockingQueue<String> queue = new ArrayBlockingQueue<>(10000);
    private final String host;
    private final int port;
    private final String graphitePrefix;
    @VisibleForTesting
    CallableSender callableSender;

    @Inject
    public HostedGraphiteSender(@Named("hosted_graphite.enable") boolean enable,
                                @Named("hosted_graphite.host") String host,
                                @Named("hosted_graphite.port") int port,
                                @Named("hosted_graphite.prefix") final String graphitePrefix)
            throws IOException {
        this.host = host;
        this.port = port;
        this.graphitePrefix = graphitePrefix;
        if (!enable) {
            logger.info("hosted graphite not enabled");
            return;
        }
        callableSender = new CallableSender();
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
        protected void startUp() throws Exception {
            if (callableSender == null) {
                return;
            }
            callableSender.connect();
            Executors.newSingleThreadExecutor().submit(callableSender);
        }

        @Override
        protected void shutDown() throws Exception {
            //todo - gfm - 9/17/14 - should this wait for the queue to drain?
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
                return;
            } catch (Exception e) {
                logger.warn("unable to send " + toSend, e);
                connect();
                stream.writeBytes(toSend);
            }
        }
    }

}
