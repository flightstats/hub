package com.flightstats.datahub.util;

import com.flightstats.datahub.model.ContentKey;
import com.flightstats.datahub.model.SequenceContentKey;
import com.google.common.base.Optional;

public class DataHubKeyRenderer {

    public Optional<ContentKey> fromString(String key) {

        //todo - gfm - 12/20/13 - do we still need this?
        try {
            Optional<? extends ContentKey> optional = Optional.of(new SequenceContentKey(Long.parseLong(key)));
            return (Optional<ContentKey>) optional;
        } catch (Exception e) {
            return Optional.absent();
        }
    }

}
