package com.flightstats.hub.rest;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.flightstats.hub.alert.AlertConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AlertConfigSerializer extends JsonSerializer<AlertConfig> {
    private final static Logger logger = LoggerFactory.getLogger(AlertConfigSerializer.class);

    @Override
    public void serialize(AlertConfig alertConfig, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        jgen.writeStartObject();
        jgen.writeStringField("name", alertConfig.getName());
        jgen.writeStringField("channel", alertConfig.getChannel());
        jgen.writeStringField("serviceName", alertConfig.getServiceName());
        jgen.writeNumberField("timeWindowMinutes", alertConfig.getTimeWindowMinutes());
        if (alertConfig.isChannelAlert()) {
            jgen.writeStringField("type", "channel");
            jgen.writeStringField("operator", alertConfig.getOperator());
            jgen.writeNumberField("threshold", alertConfig.getThreshold());
        } else {
            jgen.writeStringField("type", "group");
        }
        jgen.writeEndObject();
    }

}
