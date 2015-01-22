package com.flightstats.hub.replication;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/replication")
public class ReplicationResource {
    private final static Logger logger = LoggerFactory.getLogger(ReplicationResource.class);

    private final ReplicationService replicationService;

    private UriInfo uriInfo;

    @Inject
    public ReplicationResource(ReplicationService replicationService, UriInfo uriInfo) {
        this.replicationService = replicationService;
        this.uriInfo = uriInfo;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatus() throws Exception {
        //todo - gfm - 1/20/15 - this should return the list of replicated channels
        return null;
    }

    //todo - gfm - 1/20/15 - add get for channels from list returned in GET /

}
