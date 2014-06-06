package com.flightstats.hub.service;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.flightstats.hub.group.GroupService;
import com.flightstats.hub.group.GroupStatus;
import com.flightstats.rest.Linked;
import com.google.inject.Inject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

@Path("/groupStatus")
public class GroupStatusResource {

    private final UriInfo uriInfo;
    private final GroupService groupService;

    @Inject
    public GroupStatusResource(UriInfo uriInfo, GroupService groupService) {
        this.uriInfo = uriInfo;
        this.groupService = groupService;
    }

    @GET
    @Timed
    @ExceptionMetered
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroupStatus() {
        List<GroupStatus> groupStatus = groupService.getGroupStatus();
        Linked.Builder<List<GroupStatus>> builder = Linked.linked(groupStatus);
        builder.withLink("self", uriInfo.getRequestUri());
        return Response.ok(builder.build()).build();
    }

}
