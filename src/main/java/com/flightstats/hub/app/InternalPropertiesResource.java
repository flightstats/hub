package com.flightstats.hub.app;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.metrics.InternalTracesResource;
import com.flightstats.hub.util.SecretFilter;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Properties;
import java.util.TreeSet;

@Slf4j
@SuppressWarnings("WeakerAccess")
@Path("/internal/properties")
public class InternalPropertiesResource {
    public static final String DESCRIPTION = "Get hub properties with links to other hubs in the cluster.";
    private final SecretFilter secretFilter = HubProvider.getInstance(SecretFilter.class);


    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getTraces() {
        ObjectNode root = InternalTracesResource.serverAndServers("/internal/properties");
        try {
            ObjectNode propertyNode = root.putObject("properties");
            Properties properties = HubProperties.getProperties();
            for (Object key : new TreeSet<>(properties.keySet())) {
                String value = properties.get(key).toString();
                String possiblySensitiveValue = secretFilter.redact(key.toString(), value);
                propertyNode.put(key.toString(), possiblySensitiveValue);
            }
        } catch (Exception e) {
            log.warn("?", e);
        }
        return Response.ok(root).build();
    }
}