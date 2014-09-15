package com.flightstats.hub.app.config.metrics;

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

    @Inject
    public HostedGraphiteSender(@Named("hosted_graphite.enable") boolean enable,
                                @Named("hosted_graphite.host") String host,
                                @Named("hosted_graphite.port") int port,
                                @Named("hosted_graphite.prefix") final String graphitePrefix)
            throws IOException {
        if (!enable) {
            logger.info("hosted graphite not enabled");
            return;
        }
        Socket socket = new Socket(host, port);
        final DataOutputStream stream = new DataOutputStream(socket.getOutputStream());
        Executors.newSingleThreadExecutor().submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                while (true) {
                    try {
                        String value = queue.poll(10, TimeUnit.SECONDS);
                        if (value != null) {
                            String toSend = graphitePrefix + "." + value;
                            logger.debug("sending value {}", toSend);
                            stream.writeBytes(toSend);
                        }

                    } catch (Exception e) {
                        logger.error("unable to send value to graphite", e);
                    }
                }
            }
        });
    }

    public void send(String value) {
        try {
            logger.debug("value to send {}", value);
            queue.add(value);
        } catch (Exception e) {
            logger.warn("unable to add graphite metric to queue {}", value);
        }
    }


}
