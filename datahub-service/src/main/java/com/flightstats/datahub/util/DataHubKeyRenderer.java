package com.flightstats.datahub.util;

import com.flightstats.datahub.model.DataHubKey;

public class DataHubKeyRenderer {

    public String keyToString(DataHubKey key) {
        return Long.toString(key.getSequence());
    }

    public DataHubKey fromString(String key) {

        try {
            return new DataHubKey(Long.parseLong(key));
        } catch (Exception e) {
            throw new RuntimeException("Error converting data hub key", e);
        }
    }

}
