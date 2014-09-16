package com.flightstats.hub.service;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.group.Group;
import com.flightstats.hub.group.GroupService;
import com.flightstats.hub.group.GroupStatus;
import com.flightstats.rest.Linked;
import com.google.common.base.Optional;
import com.google.inject.Inject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

/**
 * GroupResource represents all of the interactions for Group Management.
 */
@Path("/group")
public class GroupResource {

    private final UriInfo uriInfo;
    private final GroupService groupService;
    private static final ObjectMapper mapper = new ObjectMapper();

    @Inject
    public GroupResource(UriInfo uriInfo, GroupService groupService) {
        this.uriInfo = uriInfo;
        this.groupService = groupService;
    }

    @GET
    @Timed(name = "group.get", absolute = true)
    @ExceptionMetered
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroups() {
        ObjectNode root = mapper.createObjectNode();
        ObjectNode links = root.putObject("_links");
        ObjectNode self = links.putObject("self");
        self.put("href", uriInfo.getRequestUri().toString());
        ArrayNode groupsNode = links.putArray("groups");
        Iterable<Group> groups = groupService.getGroups();
        for (Group group : groups) {
            ObjectNode groupObject = groupsNode.addObject();
            groupObject.put("name", group.getName());
            groupObject.put("href", uriInfo.getBaseUri() + "group/" + group.getName());
        }
        //todo - gfm - 6/22/14 - add inFlight list to status
        ArrayNode status = root.putArray("status");
        List<GroupStatus> groupStatus = groupService.getGroupStatus();
        for (GroupStatus groupStat : groupStatus) {
            ObjectNode object = status.addObject();
            object.put("name", groupStat.getName());
            object.put("lastCompleted", groupStat.getLastCompleted());
            object.put("channelLatest", groupStat.getChannelLatest());
        }
        return Response.ok(root).build();
    }

    @Path("/{name}")
    @GET
    @Timed(name = "group.ALL.get", absolute = true)
    @ExceptionMetered
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroup(@PathParam("name") String name) {
        Optional<Group> optionalGroup = groupService.getGroup(name);
        if (!optionalGroup.isPresent()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(getLinkedGroup(optionalGroup.get())).build();
    }

    private Linked<Group> getLinkedGroup(Group group) {
        Linked.Builder<Group> builder = Linked.linked(group);
        builder.withLink("self", uriInfo.getRequestUri());
        return builder.build();
    }

    @Path("/{name}")
    @PUT
    @Timed(name = "group.ALL.put", absolute = true)
    @ExceptionMetered
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response upsertGroup(@PathParam("name") String name, String body) {
        Group group = Group.fromJson(body).withName(name);
        Optional<Group> upsertGroup = groupService.upsertGroup(group);
        if (upsertGroup.isPresent()) {
            return Response.ok(getLinkedGroup(group)).build();
        } else {
            return Response.created(uriInfo.getRequestUri()).entity(getLinkedGroup(group)).build();
        }
    }

    @Path("/{name}")
    @DELETE
    @Timed(name = "group.ALL.delete", absolute = true)
    @ExceptionMetered
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteGroup(@PathParam("name") String name) {
        groupService.delete(name);
        return Response.status(Response.Status.ACCEPTED).build();
    }
}
