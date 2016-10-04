package com.flightstats.hub.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.model.ContentPath;
import org.joda.time.DateTime;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static com.flightstats.hub.util.StaleUtil.addStaleEntities;

@Path("/internal/webhook")
public class InternalWebhookResource {

    public static final String DESCRIPTION = "Check the staleness of webhooks.";
    private static final Long DEFAULT_STALE_AGE = TimeUnit.HOURS.toMinutes(1);

    private final static ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private final static WebhookService webhookService = HubProvider.getInstance(WebhookService.class);

    @Context
    private UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get() {
        ObjectNode root = mapper.createObjectNode();
        root.put("description", DESCRIPTION);

        ObjectNode directions = root.putObject("directions");
        directions.put("stale", "HTTP GET to /internal/webhook/stale/{age} to list webhooks that are more than {age} minutes behind.");

        ObjectNode links = root.putObject("_links");
        addLink(links, "self", uriInfo.getRequestUri().toString());
        addLink(links, "stale", uriInfo.getRequestUri().toString() + "/stale/" + DEFAULT_STALE_AGE.intValue());

        addErroringWebhooks(root);

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
            Map<DateTime, URI> staleWebhooks = new TreeMap<>(Collections.reverseOrder());
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

    private void addLink(ObjectNode node, String key, String value) {
        ObjectNode link = node.putObject(key);
        link.put("href", value);
    }

    private void addErroringWebhooks(ObjectNode root) {
        ObjectNode errors = root.putObject("errors");
        ArrayNode uris = errors.putArray("uris");
        webhookService.getAll().forEach(webhook -> {
            WebhookStatus status = webhookService.getStatus(webhook);
            if (status.getErrors().size() > 0) {
                uris.add(constructWebhookURI(webhook).toString());
            }
        });
    }

    private URI constructWebhookURI(Webhook webhook) {
        return UriBuilder.fromUri(uriInfo.getBaseUri()).path("webhook").path(webhook.getName()).build();
    }
}
