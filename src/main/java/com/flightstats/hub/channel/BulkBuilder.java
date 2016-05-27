package com.flightstats.hub.channel;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.ContentKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.SortedSet;
import java.util.function.Consumer;

class BulkBuilder {

    private final static Logger logger = LoggerFactory.getLogger(BulkBuilder.class);

    public static Response build(SortedSet<ContentKey> keys, String channel,
                                 ChannelService channelService, UriInfo uriInfo, String accept) {
        return build(keys, channel, channelService, uriInfo, accept, (builder) -> {
        });
    }

    public static Response build(SortedSet<ContentKey> keys, String channel,
                                 ChannelService channelService, UriInfo uriInfo, String accept,
                                 Consumer<Response.ResponseBuilder> headerBuilder) {
        if ("application/zip".equalsIgnoreCase(accept)) {
            return ZipBulkBuilder.build(keys, channel, channelService, headerBuilder);
        } else {
            return MultiPartBulkBuilder.build(keys, channel, channelService, uriInfo, headerBuilder);
        }
    }

    static Response buildTag(String tag, SortedSet<ChannelContentKey> keys,
                             ChannelService channelService, UriInfo uriInfo, String accept) {
        return buildTag(tag, keys, channelService, uriInfo, accept, (builder) -> {
        });
    }

    static Response buildTag(String tag, SortedSet<ChannelContentKey> keys,
                             ChannelService channelService, UriInfo uriInfo, String accept,
                             Consumer<Response.ResponseBuilder> headerBuilder) {
        if ("application/zip".equalsIgnoreCase(accept)) {
            return ZipBulkBuilder.buildTag(tag, keys, channelService, headerBuilder);
        } else {
            return MultiPartBulkBuilder.buildTag(tag, keys, channelService, uriInfo, headerBuilder);
        }
    }

}
