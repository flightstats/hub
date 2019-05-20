package com.flightstats.hub.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.app.PermissionsChecker;
import com.flightstats.hub.channel.TimeLinkUtil;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.rest.Linked;
import com.flightstats.hub.util.RequestUtils;
import com.flightstats.hub.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;

/**
 * WebhookResource represents all of the interactions for Webhook Management.
 */
@SuppressWarnings("WeakerAccess")
@Path("/webhook")
@Slf4j
public class WebhookResource {

    private final static WebhookService webhookService = HubProvider.getInstance(WebhookService.class);
    private final static ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private final static String READ_ONLY_FAILURE_MESSAGE = "attempted to %s against /webhook on node with leadership disabled %s";
    private final PermissionsChecker permissionsChecker;

    @Context
    private UriInfo uriInfo;

    @Inject
    public WebhookResource(PermissionsChecker permissionsChecker) {
        this.permissionsChecker = permissionsChecker;
    }

    static Response getWebhooks(String listName, UriInfo uriInfo) {
        try {
            ObjectNode root = mapper.createObjectNode();
            ObjectNode links = addSelfLink(root, uriInfo, false);
            ArrayNode arrayNode = links.putArray(listName);
            Collection<Webhook> webhooks = new TreeSet<>(webhookService.getAll());
            for (Webhook webhook : webhooks) {
                ObjectNode objectNode = arrayNode.addObject();
                objectNode.put("name", webhook.getName());
                objectNode.put("href", uriInfo.getRequestUri() + "/" + webhook.getName());
            }
            return Response.ok(root).build();
        } catch (Exception e) {
            log.warn("wtf?", e);
            throw e;
        }
    }

    private static ObjectNode addSelfLink(ObjectNode root, UriInfo uriInfo, boolean includeChildren) {
        ObjectNode links = root.putObject("_links");
        ObjectNode self2 = links.putObject("self");
        String uri = uriInfo.getRequestUri().toString();
        self2.put("href", uri);
        if (includeChildren) {
            links.putObject("errors").put("href", uri + "/errors");
            links.putObject("lastCompleted").put("href", uri + "/lastCompleted");
        }
        return links;
    }

    static Response getStatus(String name, boolean includeChildren, UriInfo uriInfo, BiConsumer<WebhookStatus, ObjectNode> biConsumer) {
        Optional<Webhook> webhookOptional = webhookService.get(name);
        if (!webhookOptional.isPresent()) {
            log.info("webhook not found {} ", name);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        log.debug("get webhook {} ", name);
        Webhook webhook = webhookOptional.get();
        WebhookStatus status = webhookService.getStatus(webhook);
        ObjectNode root = mapper.createObjectNode();
        addSelfLink(root, uriInfo, includeChildren);
        biConsumer.accept(status, root);
        return Response.ok(root).build();
    }

    static Response get(String name, UriInfo uriInfo) {
        return getStatus(name, true, uriInfo, ((status, root) -> {
            Webhook webhook = status.getWebhook();
            DateTime stable = TimeUtil.stable();
            root.put("name", webhook.getName());
            root.put("callbackUrl", webhook.getCallbackUrl());
            root.put("parallelCalls", webhook.getParallelCalls());
            root.put("paused", webhook.isPaused());
            root.put("batch", webhook.getBatch());
            root.put("heartbeat", webhook.isHeartbeat());
            root.put("ttlMinutes", webhook.getTtlMinutes());
            root.put("maxWaitMinutes", webhook.getMaxWaitMinutes());
            root.put("callbackTimeoutSeconds", webhook.getCallbackTimeoutSeconds());
            root.put("maxAttempts", webhook.getMaxAttempts());
            root.put("errorChannelUrl", webhook.getErrorChannelUrl());
            root.put("secondaryMetricsReporting", webhook.isSecondaryMetricsReporting());
            if (webhook.isTagPrototype()) {
                root.put("tagUrl", webhook.getTagUrl());
                root.put("isTagPrototype", webhook.isTagPrototype());
            } else {
                root.put("channelUrl", webhook.getChannelUrl());
                addLatest(status, root);
                TimeLinkUtil.addTime(root, stable, "stableTime");
                ArrayNode inFlight = root.putArray("inFlight");
                for (ContentPath contentPath : status.getInFlight()) {
                    inFlight.add(webhook.getChannelUrl() + "/" + contentPath.toUrl());
                }
                addErrors(status, root);
            }
        }));
    }

    static void addErrors(WebhookStatus status, ObjectNode root) {
        status.getErrors().forEach(root.putArray("errors")::add);
    }

    static void addLatest(WebhookStatus status, ObjectNode root) {
        Webhook webhook = status.getWebhook();
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
        }
    }

    private static Linked<Webhook> getLinked(Webhook webhook, UriInfo uriInfo) {
        Linked.Builder<Webhook> builder = Linked.linked(webhook);
        builder.withLink("self", uriInfo.getRequestUri());
        return builder.build();
    }

    static Response upsert(String name, String body, UriInfo uriInfo) {
        log.info("upsert webhook {} {}", name, body);
        Webhook webhook = Webhook.fromJson(body, webhookService.get(name)).withName(name);
        Optional<Webhook> upsert = webhookService.upsert(webhook);
        if (upsert.isPresent()) {
            return Response.ok(getLinked(webhook, uriInfo)).build();
        } else {
            return Response.created(uriInfo.getRequestUri()).entity(getLinked(webhook, uriInfo)).build();
        }
    }

    static Response deleter(String name) {
        Optional<Webhook> webhookOptional = webhookService.get(name);
        log.info("delete webhook {}", name);
        if (!webhookOptional.isPresent()) {
            log.info("webhook not found for delete {} ", name);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        webhookService.delete(name);
        return Response.status(Response.Status.ACCEPTED).build();
    }

    static Response cursorUpdater(String name, String body, UriInfo uriInfo) {
        log.info("update cursor webhook {} {}", name, body);
        Webhook webhook = Webhook.fromJson("{}", webhookService.get(name)).withName(name);
        try {
            if (RequestUtils.isValidChannelUrl(body)) {
                ContentPath item = ContentPath.fromFullUrl(body).get();
                webhookService.updateCursor(webhook, item);
            } else {
                log.info("cursor update failed.  Bad item: " + body);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        } catch (Exception e) {
            log.error("IO exception updating cursor", e);
        }
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWebhooks() {
        return getWebhooks("webhooks", uriInfo);
    }

    @Path("/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("name") String name) {
        return get(name, uriInfo);
    }

    @Path("/{name}/errors")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getErrors(@PathParam("name") String name) {
        return getStatus(name, false, uriInfo, ((status, root) -> {
            Webhook webhook = status.getWebhook();
            ArrayNode errorsNode = root.putArray("errors");
            if (webhook.isTagPrototype()) {
                String tag = RequestUtils.getTag(webhook.getTagUrl());
                Set<Webhook> tagWebhooks = TagWebhook.webhookInstancesWithTag(tag);
                for (Webhook tagWebhook : tagWebhooks) {
                    addError(webhookService.getStatus(tagWebhook), errorsNode);
                }
            } else {
                addError(status, errorsNode);
            }
        }));
    }

    private void addError(WebhookStatus status, ArrayNode nodes) {
        ObjectNode oneNode = nodes.addObject();
        oneNode.put("name", status.getWebhook().getName());
        addErrors(status, oneNode);
    }

    @Path("/{name}/lastCompleted")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLastCompleted(@PathParam("name") String name) {
        return getStatus(name, false, uriInfo, ((status, root) -> {
            Webhook webhook = status.getWebhook();
            ArrayNode lastCompleted = root.putArray("lastCompleted");
            if (webhook.isTagPrototype()) {
                String tag = RequestUtils.getTag(webhook.getTagUrl());
                Set<Webhook> tagWebhooks = TagWebhook.webhookInstancesWithTag(tag);
                for (Webhook tagWebhook : tagWebhooks) {
                    addLatest(webhookService.getStatus(tagWebhook), lastCompleted);
                }
            } else {
                addLatest(status, lastCompleted);
            }
        }));
    }

    private void addLatest(WebhookStatus status, ArrayNode nodes) {
        ObjectNode oneNode = nodes.addObject();
        oneNode.put("name", status.getWebhook().getName());
        addLatest(status, oneNode);
    }

    @Path("/{name}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response upsert(@PathParam("name") String name, String body) {
        permissionsChecker.checkWebhookLeadershipPermission(String.format(READ_ONLY_FAILURE_MESSAGE, "upsert", name));
        return upsert(name, body, uriInfo);
    }

    @Path("/{name}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("name") String name) {
        permissionsChecker.checkWebhookLeadershipPermission(String.format(READ_ONLY_FAILURE_MESSAGE, "delete", name));
        return deleter(name);
    }

    @Path("/{name}/updateCursor")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    public Response updateCursor(@PathParam("name") String name, String body) {
        permissionsChecker.checkWebhookLeadershipPermission(String.format(READ_ONLY_FAILURE_MESSAGE, "updateCursor", name));
        return cursorUpdater(name, body, uriInfo);
    }
    
}
