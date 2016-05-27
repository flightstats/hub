package com.flightstats.hub.replication;

import com.fasterxml.jackson.databind.JsonNode;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.LocalChannelService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.IOException;

@SuppressWarnings("WeakerAccess")
@Path("/internal/global/repl/{channel}")
public class InternalGlobalReplResource {

    private final static Logger logger = LoggerFactory.getLogger(InternalGlobalReplResource.class);

    private static final InternalReplicationResource resource = HubProvider.getInstance(InternalReplicationResource.class);
    private final static ChannelService channelService = HubProvider.getInstance(LocalChannelService.class);

    @POST
    public Response putPayload(@PathParam("channel") String channel, String data) throws IOException {
        logger.trace("incoming {} {}", channel, data);
        if (!channelService.channelExists(channel)) {
            //todo - gfm - 5/26/16 - get channel
            /**
             * take the url and strip off the id
             *  "id" : "2014/01/13/10/42",
             "url" : "http://hub/channel/stumptown/2014/01/13/10/42",
             */
            JsonNode node = resource.readData(channel, data);
            String channelUrl = StringUtils.removeEnd(node.get("url").asText(), node.get("id").asText());
            //todo - gfm - 5/26/16 - get remote channel config, then save it


            channelService.createChannel(null);
        }
        return resource.putPayload(channel, data);
    }

}
