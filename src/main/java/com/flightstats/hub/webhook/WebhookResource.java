package com.flightstats.hub.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.channel.TimeLinkUtil;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.rest.Linked;
import com.flightstats.hub.util.RequestUtils;
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
 * WebhookResource represents all of the interactions for Webhook Management.
 */
@SuppressWarnings("WeakerAccess")
@Path("/webhook")
public class WebhookResource {

    private final static Logger logger = LoggerFactory.getLogger(WebhookResource.class);
    private final static WebhookService webhookService = HubProvider.getInstance(WebhookService.class);
    private final static ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);

    @Context
    private UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWebhooks() {
        return getWebhooks("webhooks", uriInfo);
    }

    static Response getWebhooks(String listName, UriInfo uriInfo) {
        try {
            ObjectNode root = mapper.createObjectNode();
            ObjectNode links = addSelfLink(root, uriInfo);
            ArrayNode arrayNode = links.putArray(listName);
            Collection<Webhook> webhooks = new TreeSet<>(webhookService.getAll());
            for (Webhook webhook : webhooks) {
                ObjectNode objectNode = arrayNode.addObject();
                objectNode.put("name", webhook.getName());
                objectNode.put("href", uriInfo.getRequestUri() + "/" + webhook.getName());
            }
            return Response.ok(root).build();
        } catch (Exception e) {
            logger.warn("wtf?", e);
            throw e;
        }
    }

    private static ObjectNode addSelfLink(ObjectNode root, UriInfo uriInfo) {
        ObjectNode links = root.putObject("_links");
        ObjectNode self = links.putObject("self");
        self.put("href", uriInfo.getRequestUri().toString());
        return links;
    }

    @Path("/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("name") String name) {
        return get(name, uriInfo);
    }

    static Response get(String name, UriInfo uriInfo) {
        Optional<Webhook> webhookOptional = webhookService.get(name);
        if (!webhookOptional.isPresent()) {
            logger.info("webhook not found {} ", name);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        logger.info("get webhook {} ", name);
        Webhook webhook = webhookOptional.get();
        WebhookStatus status = webhookService.getStatus(webhook);
        DateTime stable = TimeUtil.stable();
        ObjectNode root = mapper.createObjectNode();
        addSelfLink(root, uriInfo);
        root.put("name", webhook.getName());
        root.put("callbackUrl", webhook.getCallbackUrl());
        root.put("channelUrl", webhook.getChannelUrl());
        root.put("parallelCalls", webhook.getParallelCalls());
        root.put("paused", webhook.isPaused());
        root.put("batch", webhook.getBatch());
        root.put("heartbeat", webhook.isHeartbeat());
        root.put("ttlMinutes", webhook.getTtlMinutes());
        root.put("maxWaitMinutes", webhook.getMaxWaitMinutes());
        root.put("callbackTimeoutSeconds", webhook.getCallbackTimeoutSeconds());
        addLatest(webhook, status, root, true);
        TimeLinkUtil.addTime(root, stable, "stableTime");
        ArrayNode inFlight = root.putArray("inFlight");
        for (ContentPath contentPath : status.getInFlight()) {
            inFlight.add(webhook.getChannelUrl() + "/" + contentPath.toUrl());
        }
        addErrors(status, root);
        return Response.ok(root).build();
    }

    static void addErrors(WebhookStatus status, ObjectNode root) {
        status.getErrors().forEach(root.putArray("errors")::add);
    }

    static void addLatest(Webhook webhook, WebhookStatus status, ObjectNode root, boolean includeLegacy) {
        if (status.getChannelLatest() == null) {
            root.put("channelLatest", "");
        } else {
            root.put("channelLatest", webhook.getChannelUrl() + "/" + status.getChannelLatest().toUrl());
        }
        if (status.getLastCompleted() == null) {
            root.put("lastCompleted", "");
        } else {
            String lastCompleted = webhook.getChannelUrl() + "/" + status.getLastCompleted().toUrl();
            root.put("lastCompleted", lastCompleted);
            if (includeLegacy) {
                root.put("lastCompletedCallback", lastCompleted);
            }
        }
    }

    private static Linked<Webhook> getLinked(Webhook webhook, UriInfo uriInfo) {
        Linked.Builder<Webhook> builder = Linked.linked(webhook);
        builder.withLink("self", uriInfo.getRequestUri());
        return builder.build();
    }

    @Path("/{name}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response upsert(@PathParam("name") String name, String body) {
        return upsert(name, body, uriInfo);
    }

    static Response upsert(String name, String body, UriInfo uriInfo) {
        logger.info("upsert webhook {} {}", name, body);
        Webhook webhook = Webhook.fromJson(body, webhookService.get(name)).withName(name);
        Optional<Webhook> upsert = webhookService.upsert(webhook);
        if (upsert.isPresent()) {
            return Response.ok(getLinked(webhook, uriInfo)).build();
        } else {
            return Response.created(uriInfo.getRequestUri()).entity(getLinked(webhook, uriInfo)).build();
        }
    }

    @Path("/{name}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("name") String name) {
        return deleter(name);
    }

    static Response deleter(String name) {
        Optional<Webhook> webhookOptional = webhookService.get(name);
        if (!webhookOptional.isPresent()) {
            logger.info("webhook not found for delete {} ", name);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        logger.info("delete webhook {}", name);
        webhookService.delete(name);
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @Path("/{name}/updateCursor")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    public Response updateCursor(@PathParam("name") String name, String body) {
        return cursorUpdater(name, body, uriInfo);
    }

    static Response cursorUpdater(String name, String body, UriInfo uriInfo) {
        logger.info("update cursor webhook {} {}", name, body);
        Webhook webhook = Webhook.fromJson("{}", webhookService.get(name)).withName(name);
        ObjectMapper mapper = new ObjectMapper();
        try {
            String itemUrl = body;
            if (RequestUtils.isValidChannelUrl(itemUrl)) {
                ContentPath item = ContentPath.fromFullUrl(itemUrl).get();
                webhookService.updateCursor(webhook, item);
            } else {
                logger.info("cursor update failed.  Bad item: " + itemUrl);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        } catch (Exception e) {
            logger.error("IO exception updating cursor", e);
        }
        return Response.status(Response.Status.ACCEPTED).build();
    }
}
