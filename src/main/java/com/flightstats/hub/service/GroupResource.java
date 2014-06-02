package com.flightstats.hub.service;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.flightstats.hub.group.Group;
import com.flightstats.hub.group.GroupService;
import com.flightstats.rest.HalLink;
import com.flightstats.rest.Linked;
import com.google.inject.Inject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * GroupResource represents all of the interactions for Group Management.
 */
@Path("/group")
public class GroupResource {

    private final UriInfo uriInfo;
    private final GroupService groupService;

    @Inject
    public GroupResource(UriInfo uriInfo, GroupService groupService) {
        this.uriInfo = uriInfo;
        this.groupService = groupService;
    }

    @GET
    @Timed
    @ExceptionMetered
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroups() {
        Iterable<Group> groups = groupService.getGroups();
        Linked.Builder linkedBuilder = Linked.justLinks();
        linkedBuilder.withLink("self", uriInfo.getRequestUri());
        List<HalLink> halLinks = new ArrayList<>();
        for (Group group : groups) {
            String name = group.getName();
            halLinks.add(new HalLink(name, URI.create(uriInfo.getBaseUri() + "group/" + name)));
        }
        linkedBuilder.withLinks("groups", halLinks);
        return Response.ok(linkedBuilder.build()).build();
    }

    @Path("/{name}")
    @GET
    @Timed
    @ExceptionMetered
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroup(@PathParam("name") String name) {
        //todo - gfm - 5/30/14 - handle null case
        Group group = groupService.getGroup(name);
        //todo - gfm - 5/30/14 - include self link?
        return Response.ok(group.toJson()).build();
    }

    @Path("/{name}")
    @PUT
    @Timed
    @ExceptionMetered
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response upsertGroup(@PathParam("name") String name, String body) {
        Group group = Group.fromJson(body).withName(name);
        Group upsertGroup = groupService.upsertGroup(group);
        //todo - gfm - 6/1/14 - if existing group, return 200 rather than 201
        //todo - gfm - 5/30/14 - include self link?
        return Response.created(uriInfo.getRequestUri()).entity(upsertGroup.toJson()).build();
    }

    @Path("/{name}")
    @DELETE
    @Timed
    @ExceptionMetered
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteGroup(@PathParam("name") String name) {
        groupService.delete(name);
        return Response.status(Response.Status.ACCEPTED).build();
    }
}
