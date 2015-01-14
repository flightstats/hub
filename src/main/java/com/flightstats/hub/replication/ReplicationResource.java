package com.flightstats.hub.replication;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.TreeSet;

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
        return Response.ok(replicationService.getReplicationBean()).build();
    }

    @PUT
    @Path("/{domain}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putDomain(@PathParam("domain") String domain, ReplicationDomain replicationDomain,
                              @HeaderParam("Host") String host) {
        logger.info("creating domain " + domain + " replicationConfig " + replicationDomain + " host " + host);
        if (domain.equalsIgnoreCase(host)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("The domain must be different than the host").build();
        }
        replicationDomain.setDomain(domain);
        replicationService.create(replicationDomain);
        return Response.created(uriInfo.getRequestUri()).entity(replicationDomain).build();
    }

    @GET
    @Path("/{domain}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDomain(@PathParam("domain") String domain) {
        Optional<ReplicationDomain> replicationConfig = replicationService.get(domain);
        if (!replicationConfig.isPresent()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(replicationConfig.get()).build();
    }

    @DELETE
    @Path("/{domain}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteDomain(@PathParam("domain") String domain) {
        if (replicationService.delete(domain)) {
            return Response.status(Response.Status.ACCEPTED).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).entity("Replication Domain " + domain + " not found").build();
        }
    }

    @PUT
    @Path("/{domain}/{channel}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putChannel(@PathParam("domain") String domain, @PathParam("channel") String channel,
                               @HeaderParam("Host") String host) {
        logger.info("creating domain {} channel {} host {} host {}", domain, channel, host);
        if (domain.equalsIgnoreCase(host)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("The domain must be different than the host").build();
        }
        Optional<ReplicationDomain> domainOptional = replicationService.get(domain);
        if (domainOptional.isPresent()) {
            ReplicationDomain replicationDomain = domainOptional.get();
            if (replicationDomain.getExcludeExcept().add(channel)) {
                replicationService.create(replicationDomain);
            }
        } else {
            TreeSet<String> channels = new TreeSet<>();
            channels.add(channel);
            ReplicationDomain replicationDomain = ReplicationDomain.builder()
                    .domain(domain)
                    .historicalDays(0)
                    .excludeExcept(channels)
                    .build();
            replicationService.create(replicationDomain);
        }
        return Response.created(uriInfo.getRequestUri()).build();
    }

    @GET
    @Path("/{domain}/{channel}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getChannel(@PathParam("domain") String domain, @PathParam("channel") String channel) {
        Optional<ReplicationDomain> domainOptional = replicationService.get(domain);
        if (domainOptional.isPresent()) {
            ReplicationDomain replicationDomain = domainOptional.get();
            if (replicationDomain.getExcludeExcept().contains(channel)) {
                return Response.ok(replicationService.getStatus(channel)).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Channel " + channel + "not found for Replication Domain " + domain).build();
            }
        } else {
            return Response.status(Response.Status.NOT_FOUND).entity("Replication Domain " + domain + " not found").build();
        }
    }

    @DELETE
    @Path("/{domain}/{channel}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteChannel(@PathParam("domain") String domain, @PathParam("channel") String channel) {
        Optional<ReplicationDomain> domainOptional = replicationService.get(domain);
        if (domainOptional.isPresent()) {
            ReplicationDomain replicationDomain = domainOptional.get();
            if (replicationDomain.getExcludeExcept().remove(channel)) {
                replicationService.create(replicationDomain);
            }
            return Response.status(Response.Status.ACCEPTED).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).entity("Replication Domain " + domain + " not found").build();
        }
    }

}
