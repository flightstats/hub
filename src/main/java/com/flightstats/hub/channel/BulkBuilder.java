package com.flightstats.hub.channel;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.ContentKey;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.SortedSet;
import java.util.function.Consumer;

public class BulkBuilder {

    private final MultiPartBulkBuilder multiPartBulkBuilder;
    private final ZipBulkBuilder zipBulkBuilder;

    @Inject
    public BulkBuilder(MultiPartBulkBuilder multiPartBulkBuilder, ZipBulkBuilder zipBulkBuilder) {
        this.multiPartBulkBuilder = multiPartBulkBuilder;
        this.zipBulkBuilder = zipBulkBuilder;
    }

    public Response build(SortedSet<ContentKey> keys,
                          String channel,
                          ChannelService channelService,
                          UriInfo uriInfo,
                          String accept,
                          boolean descending) {
        return build(keys, channel, channelService, uriInfo, accept, descending, (builder) -> {
        });
    }

    public Response build(SortedSet<ContentKey> keys,
                          String channel,
                          ChannelService channelService,
                          UriInfo uriInfo,
                          String accept,
                          boolean descending,
                          Consumer<Response.ResponseBuilder> headerBuilder) {
        if ("application/zip".equalsIgnoreCase(accept)) {
            return zipBulkBuilder.build(keys, channel, channelService, descending, headerBuilder);
        } else {
            return multiPartBulkBuilder.build(keys, channel, channelService, uriInfo, headerBuilder, descending);
        }
    }

    Response buildTag(String tag,
                      SortedSet<ChannelContentKey> keys,
                      ChannelService channelService,
                      UriInfo uriInfo,
                      String accept) {
        //todo - gfm - order
        return buildTag(tag, keys, channelService, uriInfo, accept, (builder) -> {
        });

    }

    public Response buildTag(String tag,
                             SortedSet<ChannelContentKey> keys,
                             ChannelService channelService,
                             UriInfo uriInfo,
                             String accept,
                             Consumer<Response.ResponseBuilder> headerBuilder) {
        //todo - gfm - order
        if ("application/zip".equalsIgnoreCase(accept)) {
            return zipBulkBuilder.buildTag(tag, keys, channelService, headerBuilder);
        } else {
            return multiPartBulkBuilder.buildTag(keys, channelService, uriInfo, headerBuilder);
        }
    }

}
