package com.flightstats.hub.spoke;

import java.net.UnknownHostException;
import java.util.Collection;

public interface SpokeClusterHealthCheck {
    void testOne(Collection<String> server) throws InterruptedException;
    boolean testAll() throws UnknownHostException;
}