package com.flightstats.hub.spoke;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StringSpokeCluster implements SpokeCluster {

    private final List<String> servers;

    public StringSpokeCluster(String spokeServers) {
        servers = Arrays.asList((String[]) spokeServers.split(","));
    }

    @Override
    public List<String> getServers() {
        return servers;
    }

    @Override
    public List<String> getRandomServers() {
        List<String> random = new ArrayList<>(servers);
        Collections.shuffle(random);
        return random;
    }
}
