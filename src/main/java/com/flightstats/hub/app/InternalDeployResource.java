package com.flightstats.hub.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.flightstats.hub.cluster.CuratorCluster;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Set;

@Path("/internal/deploy")
public class InternalDeployResource {

    public static final String DESCRIPTION = "Get a list of hubs to deploy to in a cluster.";

    private final ObjectMapper mapper;
    private final CuratorCluster curatorCluster;

    @Inject
    InternalDeployResource(ObjectMapper mapper, @Named("HubCluster") CuratorCluster curatorCluster) {
        this.mapper = mapper;
        this.curatorCluster = curatorCluster;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response deploy(@Context UriInfo uriInfo) {
        ArrayNode root = mapper.createArrayNode();
        Set<String> allServers = curatorCluster.getAllServers();
        for (String server : allServers) {
            root.add(server);
        }
        return Response.ok(root).build();
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/text")
    public Response text(@Context UriInfo uriInfo) {
        Set<String> allServers = curatorCluster.getAllServers();
        ArrayList<String> list = new ArrayList<>();
        for (String allServer : allServers) {
            list.add(StringUtils.substringBefore(allServer, ":"));
        }
        String join = StringUtils.join(list, " ");
        return Response.ok(join).build();
    }

}
