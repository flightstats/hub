package com.flightstats.hub.health;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.metrics.InternalTracesResource;
import com.flightstats.hub.rest.RestClient;
import com.flightstats.hub.util.HubUtils;
import com.sun.jersey.api.client.ClientResponse;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/internal/health")
public class InternalHealthResource {

    public static final String DESCRIPTION = "See status of all hubs in a cluster.";

    private final ObjectMapper mapper;
    private final InternalTracesResource internalTracesResource;

    @Context
    private UriInfo uriInfo;

    @Inject
    InternalHealthResource(ObjectMapper mapper, InternalTracesResource internalTracesResource) {
        this.mapper = mapper;
        this.internalTracesResource = internalTracesResource;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkHealth(@Context UriInfo uriInfo) {
        ObjectNode healthRoot = internalTracesResource.serverAndServers("/health");
        ObjectNode root = mapper.createObjectNode();
        JsonNode servers = healthRoot.get("servers");
        for (JsonNode server : servers) {
            callHealth(root, server.asText());
        }
        return Response.ok(root).build();
    }

    private void callHealth(ObjectNode root, String link) {
        ClientResponse response = null;
        try {
            response = RestClient.defaultClient().resource(link).get(ClientResponse.class);
            String string = response.getEntity(String.class);
            JsonNode jsonNode = mapper.readTree(string);
            root.set(link, jsonNode);
        } catch (Exception e) {
            root.put(link, "unable to get response " + e.getMessage());
        } finally {
            HubUtils.close(response);
        }
    }

}
