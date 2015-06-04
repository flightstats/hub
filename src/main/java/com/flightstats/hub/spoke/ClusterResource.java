package com.flightstats.hub.spoke;

import com.google.inject.Inject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/cluster")
public class ClusterResource {

    @Inject
    CuratorSpokeCluster curatorSpokeCluster;

    @GET
    public Response getChannelMetadata() {
        return Response.ok(curatorSpokeCluster.getServers().toString()).build();
    }

}
