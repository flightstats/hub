package com.flightstats.datahub.app.config;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.ChannelCreationRequest;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.flightstats.datahub.model.serialize.ChannelConfigurationMixIn;
import com.flightstats.datahub.model.serialize.ChannelCreationRequestMixIn;
import com.flightstats.datahub.model.serialize.ValueInsertionResultMixIn;
import com.flightstats.rest.*;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.module.SimpleModule;

import java.util.Date;

public class DataHubObjectMapperFactory {

    public ObjectMapper build() {

        SimpleModule module = new SimpleModule("data hub", new Version(1, 0, 0, null));
        module.addSerializer(HalLinks.class, new HalLinksSerializer());
        module.addSerializer(Date.class, new Rfc3339DateSerializer());

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        objectMapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
        objectMapper.registerModule(module);
        objectMapper.getDeserializationConfig().addMixInAnnotations(ChannelCreationRequest.class, ChannelCreationRequestMixIn.class);
        objectMapper.getDeserializationConfig().addMixInAnnotations(ChannelConfiguration.class, ChannelConfigurationMixIn.class);
        objectMapper.getSerializationConfig().addMixInAnnotations(ValueInsertionResult.class, ValueInsertionResultMixIn.class);
        objectMapper.getSerializationConfig().addMixInAnnotations(Linked.class, LinkedMixIn.class);

        return objectMapper;
    }
}
