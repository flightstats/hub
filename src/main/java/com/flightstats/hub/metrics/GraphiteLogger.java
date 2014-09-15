package com.flightstats.hub.metrics;

import com.codahale.metrics.graphite.Graphite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GraphiteLogger {

    private final static Logger logger = LoggerFactory.getLogger(GraphiteLogger.class);
    private Graphite graphite;

    public GraphiteLogger(Graphite graphite) {
        this.graphite = graphite;
    }

    public void connect() throws IllegalStateException, IOException {
        graphite.connect();
    }

    public void send(String name, String value, long timestamp) throws IOException {
        try {
            logger.debug("sending {} {} {}", name, value, timestamp);
            graphite.send(name, value, timestamp);
        } catch (Exception e) {
            logger.warn("exception!", e);
            throw e;
        }
    }

    public void close() throws IOException {
        graphite.close();
    }

    public int getFailures() {
        return graphite.getFailures();
    }
}
