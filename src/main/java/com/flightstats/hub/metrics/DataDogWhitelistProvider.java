package com.flightstats.hub.metrics;

import com.flightstats.hub.app.HubProperties;
import com.google.inject.Provider;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DataDogWhitelistProvider implements Provider<DataDogWhitelist> {

    @Override
    public DataDogWhitelist get() {
        String patterns = HubProperties.getProperty("metrics.filter.include.patterns", "");
        List<String> whitelist = Arrays.stream(
                patterns
                .split(",", 0))
                .collect(Collectors.toList());
        return new DataDogWhitelist(whitelist);
    }
}
