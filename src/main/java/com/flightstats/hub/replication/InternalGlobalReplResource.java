package com.flightstats.hub.replication;

import com.fasterxml.jackson.databind.JsonNode;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.LocalChannelService;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.HubUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/internal/global/repl/{channel}")
public class InternalGlobalReplResource {

    private final static Logger logger = LoggerFactory.getLogger(InternalGlobalReplResource.class);

    private static final InternalReplicationResource resource = HubProvider.getInstance(InternalReplicationResource.class);
    private static final ChannelService channelService = HubProvider.getInstance(LocalChannelService.class);
    private static final HubUtils hubUtils = HubProvider.getInstance(HubUtils.class);

    @POST
    public Response putPayload(@PathParam("channel") String channel, String data) throws IOException {
        logger.trace("incoming {} {}", channel, data);
        if (!channelService.channelExists(channel)) {
            JsonNode node = resource.readData(channel, data);
            String channelUrl = StringUtils.removeEnd(node.get("url").asText(), node.get("id").asText());
            ChannelConfig masterConfig = hubUtils.getChannel(channelUrl);
            masterConfig.getGlobal().setIsMaster(false);
            channelService.createChannel(masterConfig);
        }
        return resource.putPayload(channel, data);
    }

}
