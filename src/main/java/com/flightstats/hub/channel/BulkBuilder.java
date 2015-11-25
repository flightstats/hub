package com.flightstats.hub.channel;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ContentKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.SortedSet;

public class BulkBuilder {

    private final static Logger logger = LoggerFactory.getLogger(BulkBuilder.class);

    public static Response build(SortedSet<ContentKey> keys, String channel,
                                 ChannelService channelService, UriInfo uriInfo, String accept) {
        if ("application/zip".equalsIgnoreCase(accept)) {
            return ZipBulkBuilder.build(keys, channel, channelService);
        } else {
            return MultiPartBulkBuilder.build(keys, channel, channelService, uriInfo);
        }
    }

}
