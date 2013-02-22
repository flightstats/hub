package com.flightstats.datahub.model.serialize;

import com.flightstats.datahub.model.DataHubKey;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Date;

public abstract class ChannelConfigurationMixIn {

    public ChannelConfigurationMixIn(@JsonProperty("name") String name, @JsonProperty("creationDate") Date creationDate,
                                     @JsonProperty("lastUpdateKey") DataHubKey lastUpdateKey) {
        throw new IllegalStateException("Do not instantiate mix-in configuration classes.");
    }

    @JsonIgnore
    public Date getLastUpdateDate() {
        throw new UnsupportedOperationException();
    }

}
