package com.flightstats.hub.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.model.ContentPath;
import org.joda.time.DateTime;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import static com.flightstats.hub.util.StaleUtil.addStaleEntities;

@Path("/internal/webhook")
public class InternalWebhookResource {

    public static final String DESCRIPTION = "Get all webhooks, or stale or erroring webhooks.";
    private static final Long DEFAULT_STALE_AGE = TimeUnit.HOURS.toMinutes(1);

    private final ObjectMapper mapper;
    private final WebhookService webhookService;
    private final LocalWebhookManager localWebhookManager;
    private final WebhookResource webhookResource;

    @Context
    private UriInfo uriInfo;

    @Inject
    InternalWebhookResource(ObjectMapper mapper,
                            WebhookService webhookService,
                            LocalWebhookManager localWebhookManager,
                            WebhookResource webhookResource) {
        this.mapper = mapper;
        this.webhookService = webhookService;
        this.localWebhookManager = localWebhookManager;
        this.webhookResource = webhookResource;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get() {
        ObjectNode root = mapper.createObjectNode();
        root.put("description", DESCRIPTION);

        ObjectNode directions = root.putObject("directions");
        directions.put("configs", "HTTP GET to /internal/webhook/configs to list all webhook configurations");
        directions.put("stale", "HTTP GET to /internal/webhook/stale/{age} to list webhooks that are more than {age} minutes behind.");
        directions.put("errors", "HTTP GET to /internal/webhook/errors to list all webhooks with recent errors.");
        directions.put("run/{name}", "HTTP PUT to /internal/webhook/run/{name} to start processing this webhook.");
        directions.put("delete/{name}", "HTTP PUT to /internal/webhook/delete/{name} to stop processing this webhook on this server.");

        ObjectNode links = root.putObject("_links");
        addLink(links, "self", uriInfo.getRequestUri().toString());
        addLink(links, "configs", uriInfo.getRequestUri().toString() + "/configs");
        addLink(links, "stale", uriInfo.getRequestUri().toString() + "/stale/" + DEFAULT_STALE_AGE.intValue());
        addLink(links, "errors", uriInfo.getRequestUri().toString() + "/errors");

        return Response.ok(root).build();
    }

    @GET
    @Path("/stale/{age}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response stale(@PathParam("age") int age) {
        ObjectNode root = mapper.createObjectNode();
        ObjectNode links = root.putObject("_links");
        addLink(links, "self", uriInfo.getRequestUri().toString());
        addStaleEntities(root, age, (staleCutoff) -> {
            Map<DateTime, URI> staleWebhooks = new TreeMap<>();
            webhookService.getAll().forEach(webhook -> {
                WebhookStatus status = webhookService.getStatus(webhook);
                ContentPath contentPath = status.getLastCompleted();
                if (contentPath.getTime().isAfter(staleCutoff)) return;

                URI webhookURI = constructWebhookURI(webhook);
                staleWebhooks.put(contentPath.getTime(), webhookURI);
            });
            return staleWebhooks;
        });
        return Response.ok(root).build();
    }

    @GET
    @Path("/configs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response configs(){
        ObjectNode root = mapper.createObjectNode();
        ArrayNode arrayNode = root.putArray("webhooks");
        Collection<Webhook> webhooks = new TreeSet<>(webhookService.getAll());
        for (Webhook webhook : webhooks) {
            ObjectNode objectNode = arrayNode.addObject();
            objectNode.put("name", webhook.getName());
            objectNode.put("callbackUrl", webhook.getCallbackUrl());
            objectNode.put("channelUrl", webhook.getChannelUrl());
            objectNode.put("parallelCalls", webhook.getParallelCalls());
            objectNode.put("paused", webhook.isPaused());
            objectNode.put("batch", webhook.getBatch());
            objectNode.put("heartbeat", webhook.isHeartbeat());
            objectNode.put("ttlMinutes", webhook.getTtlMinutes());
            objectNode.put("maxWaitMinutes", webhook.getMaxWaitMinutes());
        }
        return Response.ok(root).build();
    }

    @GET
    @Path("/errors")
    @Produces(MediaType.APPLICATION_JSON)
    public Response errors() {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode uris = root.putArray("webhooks");
        webhookService.getAll().forEach(webhook -> {
            WebhookStatus status = webhookService.getStatus(webhook);
            if (status.getErrors().size() > 0) {
                ObjectNode node = uris.addObject();
                node.put("name", webhook.getName());
                node.put("href", constructWebhookURI(webhook).toString());
                node.put("callbackUrl", webhook.getCallbackUrl());
                node.put("channelUrl", webhook.getChannelUrl());
                webhookResource.addLatest(status, node);
                webhookResource.addErrors(status, node);
            }
        });
        return Response.ok(root).build();
    }

    private void addLink(ObjectNode node, String key, String value) {
        node.putObject(key).put("href", value);
    }

    private URI constructWebhookURI(Webhook webhook) {
        return UriBuilder.fromUri(uriInfo.getBaseUri()).path("webhook").path(webhook.getName()).build();
    }

    @PUT
    @Path("/run/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response run(@PathParam("name") String name) {
        if (localWebhookManager.ensureRunning(name)) {
            return Response.ok().build();
        }
        return Response.status(400).build();
    }

    @PUT
    @Path("/delete/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("name") String name) {
        localWebhookManager.stopLocal(name, true);
        return Response.ok().build();
    }

    @GET
    @Path("/count")
    public Response count() {
        return Response.ok(localWebhookManager.getCount()).build();
    }

}
