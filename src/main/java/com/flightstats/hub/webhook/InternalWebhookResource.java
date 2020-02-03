package com.flightstats.hub.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.PermissionsChecker;
import com.flightstats.hub.config.properties.LocalHostProperties;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.util.StaleEntity;
import lombok.extern.slf4j.Slf4j;
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

import static com.flightstats.hub.constant.InternalResourceDescription.WEBHOOK_DESCRIPTION;

@Path("/internal/webhook")
@Slf4j
public class InternalWebhookResource {

    private final static String READ_ONLY_FAILURE_MESSAGE = "attempted to internally %s for webhook on node with leadership disabled %s";
    private static final Long DEFAULT_STALE_AGE = TimeUnit.HOURS.toMinutes(1);

    private final PermissionsChecker permissionsChecker;
    private final LocalHostProperties localHostProperties;
    private final WebhookService webhookService;
    private final LocalWebhookRunner localWebhookRunner;
    private final StaleEntity staleEntity;
    private final ObjectMapper objectMapper;


    @Context
    private UriInfo uriInfo;

    @Inject
    public InternalWebhookResource(PermissionsChecker permissionsChecker,
                                   LocalHostProperties localHostProperties,
                                   WebhookService webhookService,
                                   LocalWebhookRunner localWebhookRunner,
                                   StaleEntity staleEntity,
                                   ObjectMapper objectMapper) {
        this.permissionsChecker = permissionsChecker;
        this.localHostProperties = localHostProperties;
        this.webhookService = webhookService;
        this.localWebhookRunner = localWebhookRunner;
        this.staleEntity = staleEntity;
        this.objectMapper = objectMapper;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get() {
        final ObjectNode root = objectMapper.createObjectNode();
        root.put("description", WEBHOOK_DESCRIPTION);

        final ObjectNode directions = root.putObject("directions");
        directions.put("configs", "HTTP GET to /internal/webhook/configs to list all webhook configurations");
        directions.put("stale", "HTTP GET to /internal/webhook/stale/{age} to list webhooks that are more than {age} minutes behind.");
        directions.put("errors", "HTTP GET to /internal/webhook/errors to list all webhooks with recent errors.");
        directions.put("running", "HTTP GET to /internal/webhook/running to list all webhooks that are running on this server.");
        directions.put("run/{name}", "HTTP PUT to /internal/webhook/run/{name} to start processing this webhook.");
        directions.put("delete/{name}", "HTTP PUT to /internal/webhook/delete/{name} to stop processing this webhook on this server.");

        final ObjectNode links = root.putObject("_links");
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
        final ObjectNode root = objectMapper.createObjectNode();
        final ObjectNode links = root.putObject("_links");
        addLink(links, "self", uriInfo.getRequestUri().toString());
        staleEntity.add(root, age, (staleCutoff) -> {
            Map<DateTime, URI> staleWebhooks = new TreeMap<>();
            webhookService.getAll().forEach(webhook -> {
                final WebhookStatus status = webhookService.getStatus(webhook);
                final ContentPath contentPath = status.getLastCompleted();
                if (contentPath.getTime().isAfter(staleCutoff)) return;

                final URI webhookURI = constructWebhookURI(webhook);
                staleWebhooks.put(contentPath.getTime(), webhookURI);
            });
            return staleWebhooks;
        });
        return Response.ok(root).build();
    }

    @GET
    @Path("/configs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response configs() {
        ObjectNode root = objectMapper.createObjectNode();
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
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode uris = root.putArray("webhooks");
        webhookService.getAll().forEach(webhook -> {
            final WebhookStatus status = webhookService.getStatus(webhook);
            if (status.getErrors() != null && status.getErrors().size() > 0) {
                ObjectNode node = uris.addObject();
                node.put("name", webhook.getName());
                node.put("href", constructWebhookURI(webhook).toString());
                node.put("callbackUrl", webhook.getCallbackUrl());
                node.put("channelUrl", webhook.getChannelUrl());
                WebhookResource.addLatest(status, node);
                WebhookResource.addErrors(status, node);
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
        if (!permissionsChecker.checkWebhookLeadershipPermission(String.format(READ_ONLY_FAILURE_MESSAGE, "run", name), false)) {
            return Response.status(400).build();  // TODO: Fix Hub cluster being assumed to contain only webhook-leadership-eligible nodes.
        }
        return attemptRun(name);
    }

    @PUT
    @Path("/delete/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("name") String name) {
        permissionsChecker.checkWebhookLeadershipPermission(String.format(READ_ONLY_FAILURE_MESSAGE, "delete", name));
        return attemptDelete(name);
    }

    @GET
    @Path("/count")
    public Response count() {
        return Response.ok(localWebhookRunner.getCount()).build();
    }

    @GET
    @Path("/running")
    @Produces(MediaType.APPLICATION_JSON)
    public Response running() {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode arrayNode = root.putArray(localHostProperties.getName());
        localWebhookRunner.getRunning().forEach(arrayNode::add);

        return Response.ok(root).build();
    }

    private Response attemptRun(String name) {
        if (localWebhookRunner.ensureRunning(name)) {
            return Response.ok().build();
        }
        return Response.status(400).build();
    }

    private Response attemptDelete(String name) {
        localWebhookRunner.stop(name, false);
        return Response.ok().build();
    }

}
