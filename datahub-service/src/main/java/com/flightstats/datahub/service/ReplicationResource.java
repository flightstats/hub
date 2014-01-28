package com.flightstats.datahub.service;

import com.flightstats.datahub.replication.ReplicationConfig;
import com.flightstats.datahub.replication.ReplicationService;
import com.flightstats.datahub.replication.ReplicationStatus;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;

/**
 *
 */
@Path("/replication")
public class ReplicationResource {
    //todo - gfm - 1/27/14 - ad more integration tests
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
        Collection<ReplicationStatus> status = replicationService.getStatus();
        return Response.ok(status).build();
    }

    @PUT
    @Path("/{domain}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putDomain(@PathParam("domain") String domain, ReplicationConfig replicationConfig) {
        logger.info("creating domain " + domain + " replicationConfig " + replicationConfig);
        replicationService.create(domain, replicationConfig);
        return Response.created(uriInfo.getRequestUri()).entity(replicationConfig).build();
    }

    @GET
    @Path("/{domain}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDomain(@PathParam("domain") String domain) {
        Optional<ReplicationConfig> replicationConfig = replicationService.get(domain);
        if (!replicationConfig.isPresent()) {
            Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(replicationConfig.get()).build();
    }

    @DELETE
    @Path("/{domain}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteDomain(@PathParam("domain") String domain) {
        replicationService.delete(domain);
        return Response.ok().build();
    }


}
