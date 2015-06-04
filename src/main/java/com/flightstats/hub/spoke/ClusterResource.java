package com.flightstats.hub.spoke;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/cluster")
public class ClusterResource {

    private final static Logger logger = LoggerFactory.getLogger(ClusterResource.class);

    @Inject
    CuratorSpokeCluster curatorSpokeCluster;

    @GET
    public Response getCluster() {
        List<String> servers = curatorSpokeCluster.getServers();
        logger.info("get cluster {}", servers);
        return Response.ok(servers.toString()).build();
    }

}
