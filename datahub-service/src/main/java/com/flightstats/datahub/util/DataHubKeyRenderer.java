package com.flightstats.datahub.util;

import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.SequenceDataHubKey;
import com.google.common.base.Optional;

public class DataHubKeyRenderer {

    public Optional<DataHubKey> fromString(String key) {

        //todo - gfm - 12/20/13 - do we still need this?
        try {
            Optional<? extends DataHubKey> optional = Optional.of(new SequenceDataHubKey(Long.parseLong(key)));
            return (Optional<DataHubKey>) optional;
        } catch (Exception e) {
            return Optional.absent();
        }
    }

}
