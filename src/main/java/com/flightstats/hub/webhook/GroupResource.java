package com.flightstats.hub.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.channel.TimeLinkUtil;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.rest.Linked;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;
import java.util.TreeSet;

/**
 * GroupResource represents all of the interactions for Group Management.
 */
@SuppressWarnings("WeakerAccess")
@Path("/group")
public class GroupResource {

    private final static Logger logger = LoggerFactory.getLogger(GroupResource.class);
    private final GroupService groupService = HubProvider.getInstance(GroupService.class);
    @Context
    private UriInfo uriInfo;
    private ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroups() {
        ObjectNode root = mapper.createObjectNode();
        ObjectNode links = addSelfLink(root);
        ArrayNode groupsNode = links.putArray("groups");
        Collection<Group> groups = new TreeSet<>(groupService.getGroups());
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroup(@PathParam("name") String name) {
        Optional<Group> optionalGroup = groupService.getGroup(name);
        if (!optionalGroup.isPresent()) {
            logger.info("group not found {} ", name);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        logger.info("get group {} ", name);
        Group group = optionalGroup.get();
        GroupStatus status = groupService.getGroupStatus(group);
        DateTime stable = TimeUtil.stable();
        ObjectNode root = mapper.createObjectNode();
        addSelfLink(root);
        root.put("name", group.getName());
        root.put("callbackUrl", group.getCallbackUrl());
        root.put("channelUrl", group.getChannelUrl());
        root.put("parallelCalls", group.getParallelCalls());
        root.put("paused", group.isPaused());
        root.put("batch", group.getBatch());
        root.put("heartbeat", group.isHeartbeat());
        root.put("ttlMinutes", group.getTtlMinutes());
        root.put("maxWaitMinutes", group.getMaxWaitMinutes());
        String lastCompleted = "";
        if (status.getLastCompleted() != null) {
            lastCompleted = group.getChannelUrl() + "/" + status.getLastCompleted().toUrl();
        }
        root.put("lastCompletedCallback", lastCompleted);
        root.put("lastCompleted", lastCompleted);
        if (status.getChannelLatest() == null) {
            root.put("channelLatest", "");
        } else {
            root.put("channelLatest", group.getChannelUrl() + "/" + status.getChannelLatest().toUrl());
        }
        TimeLinkUtil.addTime(root, stable, "stableTime");
        ArrayNode inFlight = root.putArray("inFlight");
        for (ContentPath contentPath : status.getInFlight()) {
            inFlight.add(group.getChannelUrl() + "/" + contentPath.toUrl());
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
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response upsertGroup(@PathParam("name") String name, String body) {
        logger.info("upsert group {} {}", name, body);
        Group group = Group.fromJson(body, groupService.getGroup(name)).withName(name);
        Optional<Group> upsertGroup = groupService.upsertGroup(group);
        if (upsertGroup.isPresent()) {
            return Response.ok(getLinkedGroup(group)).build();
        } else {
            return Response.created(uriInfo.getRequestUri()).entity(getLinkedGroup(group)).build();
        }
    }

    @Path("/{name}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteGroup(@PathParam("name") String name) {
        logger.info("delete group {}", name);
        groupService.delete(name);
        return Response.status(Response.Status.ACCEPTED).build();
    }
}
