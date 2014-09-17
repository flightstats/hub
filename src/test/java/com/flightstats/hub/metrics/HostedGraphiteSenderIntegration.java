package com.flightstats.hub.metrics;

import com.flightstats.hub.util.Sleeper;

import java.io.IOException;

public class HostedGraphiteSenderIntegration {

    /**
     * This is useful for verifying that HostedGraphiteSender will retry after losing connectivity.
     * Not sure how to turn this into a useful unit test.
     */
    public static void main(String[] args) throws IOException {
        HostedGraphiteSender sender = new HostedGraphiteSender(true, "carbon.hostedgraphite.com", 2003,
                "36067340-1113-4cfb-b922-4ebfa86c6457.hub.local.HostedGraphiteSenderTest");
        sender.callableSender.connect();
        for (int i = 0; i < 100; i++) {
            sender.callableSender.send("stuff " + i);
            Sleeper.sleep(1000);
        }
    }


}