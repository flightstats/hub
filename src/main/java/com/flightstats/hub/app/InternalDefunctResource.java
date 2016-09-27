package com.flightstats.hub.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ContentKey;
import com.google.common.base.Optional;
import org.joda.time.DateTime;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

@Path("/internal/defunct/")
public class InternalDefunctResource {

    public static final String DESCRIPTION = "Get a list of entities deemed defunct.";
    public static final Long DEFUNCT_MINUTES = TimeUnit.DAYS.toMinutes(7);

    private final static ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private final static ChannelService channelService = HubProvider.getInstance(ChannelService.class);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get() {
        ObjectNode root = mapper.createObjectNode();
        root.put("description", DESCRIPTION);
        root.put("defunct minutes", DEFUNCT_MINUTES);

        ArrayNode channels = root.putArray("channels");
        DateTime defunctCutoff = DateTime.now().minusMinutes(DEFUNCT_MINUTES.intValue());
        channelService.getChannels().forEach(channelConfig -> {
            Optional<ContentKey> optionalContentKey = channelService.getLatest(channelConfig.getName(), false, false);
            if (!optionalContentKey.isPresent()) return;
            ContentKey contentKey = optionalContentKey.get();

            if (contentKey.getTime().isAfter(defunctCutoff)) return;
            channels.add(channelConfig.getName());
        });

        return Response.ok(root).build();
    }
}
