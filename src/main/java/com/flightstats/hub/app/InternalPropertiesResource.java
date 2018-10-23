package com.flightstats.hub.app;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.metrics.InternalTracesResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Properties;
import java.util.TreeSet;

@Path("/internal/properties")
public class InternalPropertiesResource {

    public final static String DESCRIPTION = "Get hub properties with links to other hubs in the cluster.";
    private final static Logger logger = LoggerFactory.getLogger(InternalPropertiesResource.class);

    private final HubProperties hubProperties;
    private final InternalTracesResource internalTracesResource;

    @Inject
    InternalPropertiesResource(HubProperties hubProperties, InternalTracesResource internalTracesResource) {
        this.hubProperties = hubProperties;
        this.internalTracesResource = internalTracesResource;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getTraces() {
        ObjectNode root = internalTracesResource.serverAndServers("/internal/properties");
        try {
            ObjectNode properyNode = root.putObject("properties");
            Properties properties = hubProperties.getProperties();
            for (Object key : new TreeSet<>(properties.keySet())) {
                Object value = properties.get(key);
                properyNode.put(key.toString(), value.toString());
            }
        } catch (Exception e) {
            logger.warn("?", e);
        }
        return Response.ok(root).build();
    }

}