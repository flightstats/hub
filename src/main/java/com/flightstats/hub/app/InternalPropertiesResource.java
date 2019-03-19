package com.flightstats.hub.app;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.metrics.InternalTracesResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Properties;
import java.util.TreeSet;
import java.util.stream.Stream;

@SuppressWarnings("WeakerAccess")
@Path("/internal/properties")
public class InternalPropertiesResource {
    public static final String DESCRIPTION = "Get hub properties with links to other hubs in the cluster.";
    private final static Logger logger = LoggerFactory.getLogger(InternalPropertiesResource.class);
    private final static String redacted = "[REDACTED]";
    private final Stream<String> matchers = Stream.of("app_key", "api_key", "password");
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getTraces() {
        ObjectNode root = InternalTracesResource.serverAndServers("/internal/properties");
        try {
            ObjectNode propertyNode = root.putObject("properties");
            Properties properties = HubProperties.getProperties();
            for (Object key : new TreeSet<>(properties.keySet())) {
                Object value = properties.get(key);
                String possiblySensitiveValue = redactionFilter(key.toString(), value.toString());
                propertyNode.put(key.toString(), possiblySensitiveValue);
            }
        } catch (Exception e) {
            logger.warn("?", e);
        }
        return Response.ok(root).build();
    }

    private String redactionFilter(String key, String property) {
        boolean shouldRedact = matchers.anyMatch(key::contains);
        return shouldRedact ? redacted : property;
    }

}