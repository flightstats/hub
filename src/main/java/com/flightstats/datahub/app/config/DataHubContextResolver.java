package com.flightstats.datahub.app.config;

import com.flightstats.datahub.model.ChannelCreationRequest;
import com.flightstats.datahub.model.serialize.ChannelCreationRequestMixIn;
import com.flightstats.rest.HalLinks;
import com.flightstats.rest.HalLinksSerializer;
import com.flightstats.rest.Linked;
import com.flightstats.rest.LinkedMixIn;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.module.SimpleModule;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
public class DataHubContextResolver implements ContextResolver<ObjectMapper> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public DataHubContextResolver() {
        SimpleModule module = new SimpleModule("users", new Version(1, 0, 0, null));
        module.addSerializer(HalLinks.class, new HalLinksSerializer());

        objectMapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        objectMapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
        objectMapper.registerModule(module);
        objectMapper.getDeserializationConfig().addMixInAnnotations(ChannelCreationRequest.class, ChannelCreationRequestMixIn.class);
        objectMapper.getSerializationConfig().addMixInAnnotations(Linked.class, LinkedMixIn.class);
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return objectMapper;
    }
}
