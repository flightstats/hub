package com.flightstats.hub.group;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.metrics.EventTimed;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.rest.Linked;
import com.google.common.base.Optional;
import com.google.inject.Inject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * GroupResource represents all of the interactions for Group Management.
 */
@Path("/group")
public class GroupResource {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final UriInfo uriInfo;
    private final GroupService groupService;

    @Inject
    public GroupResource(UriInfo uriInfo, GroupService groupService) {
        this.uriInfo = uriInfo;
        this.groupService = groupService;
    }

    @GET
    @EventTimed(name = "groups.get")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroups() {
        ObjectNode root = mapper.createObjectNode();
        ObjectNode links = addSelfLink(root);
        ArrayNode groupsNode = links.putArray("groups");
        Iterable<Group> groups = groupService.getGroups();
        for (Group group : groups) {
            ObjectNode groupObject = groupsNode.addObject();
            groupObject.put("name", group.getName());
            groupObject.put("href", uriInfo.getBaseUri() + "group/" + group.getName());
        }
        return Response.ok(root).build();
    }

    private ObjectNode addSelfLink(ObjectNode root) {
        ObjectNode links = root.putObject("_links");
        ObjectNode self = links.putObject("self");
        self.put("href", uriInfo.getRequestUri().toString());
        return links;
    }

    @Path("/{name}")
    @GET
    @EventTimed(name = "group.ALL.get")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroup(@PathParam("name") String name) {
        Optional<Group> optionalGroup = groupService.getGroup(name);
        if (!optionalGroup.isPresent()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        Group group = optionalGroup.get();
        GroupStatus status = groupService.getGroupStatus(group);
        ObjectNode root = mapper.createObjectNode();
        addSelfLink(root);
        root.put("name", group.getName());
        root.put("callbackUrl", group.getCallbackUrl());
        root.put("channelUrl", group.getChannelUrl());
        root.put("parallelCalls", group.getParallelCalls());
        if (status.getLastCompleted() == null) {
            root.put("lastCompletedCallback", "");
        } else {
            root.put("lastCompletedCallback", group.getChannelUrl() + "/" + status.getLastCompleted().toUrl());
        }
        if (status.getChannelLatest() == null) {
            root.put("channelLatest", "");
        } else {
            root.put("channelLatest", group.getChannelUrl() + "/" + status.getChannelLatest().toUrl());
        }
        ArrayNode inFlight = root.putArray("inFlight");
        for (ContentKey key : status.getInFlight()) {
            inFlight.add(group.getChannelUrl() + "/" + key.toUrl());
        }
        ArrayNode errors = root.putArray("errors");
        for (String error : status.getErrors()) {
            errors.add(error);
        }
        return Response.ok(root).build();
    }

    private Linked<Group> getLinkedGroup(Group group) {
        Linked.Builder<Group> builder = Linked.linked(group);
        builder.withLink("self", uriInfo.getRequestUri());
        return builder.build();
    }

    @Path("/{name}")
    @PUT
    @EventTimed(name = "group.ALL.put")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response upsertGroup(@PathParam("name") String name, String body) {
        Group group = Group.fromJson(body).withName(name).withStartingKey(new ContentKey());
        Optional<Group> upsertGroup = groupService.upsertGroup(group);
        if (upsertGroup.isPresent()) {
            return Response.ok(getLinkedGroup(group)).build();
        } else {
            return Response.created(uriInfo.getRequestUri()).entity(getLinkedGroup(group)).build();
        }
    }

    @Path("/{name}")
    @DELETE
    @EventTimed(name = "group.ALL.delete")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteGroup(@PathParam("name") String name) {
        groupService.delete(name);
        return Response.status(Response.Status.ACCEPTED).build();
    }
}
