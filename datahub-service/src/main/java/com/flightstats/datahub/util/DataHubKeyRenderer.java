package com.flightstats.datahub.util;

import com.flightstats.datahub.model.DataHubKey;
import com.google.common.base.Optional;

public class DataHubKeyRenderer {

    public String keyToString(DataHubKey key) {
        return Long.toString(key.getSequence());
    }

    public Optional<DataHubKey> fromString(String key) {

        try {
            return Optional.of(new DataHubKey(Long.parseLong(key)));
        } catch (Exception e) {
            return Optional.absent();
        }
    }

}
