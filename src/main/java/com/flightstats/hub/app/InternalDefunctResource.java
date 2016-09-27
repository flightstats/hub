package com.flightstats.hub.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.webhook.WebhookService;
import com.flightstats.hub.webhook.WebhookStatus;
import com.google.common.base.Optional;
import org.joda.time.DateTime;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

@Path("/internal/defunct")
public class InternalDefunctResource {

    public static final String DESCRIPTION = "Get a list of entities deemed defunct.";
    public static final Long DEFUNCT_MINUTES = TimeUnit.DAYS.toMinutes(7);

    private final static ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private final static ChannelService channelService = HubProvider.getInstance(ChannelService.class);
    private final static WebhookService webhookService = HubProvider.getInstance(WebhookService.class);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get() {
        return getResponseForAge(DEFUNCT_MINUTES.intValue());
    }

    @GET
    @Path("/{age}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("age") int age) {
        return getResponseForAge(age);
    }

    private Response getResponseForAge(int age) {
        DateTime defunctCutoff = DateTime.now().minusMinutes(age);

        ObjectNode root = mapper.createObjectNode();
        root.put("description", DESCRIPTION);
        root.put("defunct minutes", age);
        addDefunctChannels(root, defunctCutoff);
        addDefunctWebhooks(root, defunctCutoff);

        return Response.ok(root).build();
    }

    private void addDefunctChannels(ObjectNode root, DateTime defunctCutoff) {
        ArrayNode channels = root.putArray("channels");
        channelService.getChannels().forEach(channelConfig -> {
            Optional<ContentKey> optionalContentKey = channelService.getLatest(channelConfig.getName(), false, false);
            if (!optionalContentKey.isPresent()) return;
            ContentKey contentKey = optionalContentKey.get();

            if (contentKey.getTime().isAfter(defunctCutoff)) return;
            channels.add(channelConfig.getName());
        });
    }

    private void addDefunctWebhooks(ObjectNode root, DateTime defunctCutoff) {
        ArrayNode webhooks = root.putArray("webhooks");
        webhookService.getAll().forEach(webhook -> {
            WebhookStatus status = webhookService.getStatus(webhook);
            ContentPath contentPath = status.getLastCompleted();

            if (contentPath.getTime().isAfter(defunctCutoff)) return;
            webhooks.add(webhook.getName());
        });
    }
}
