package com.flightstats.hub.service;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.config.metrics.EventTimed;
import com.flightstats.hub.app.config.metrics.PerChannelTimed;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Request;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.sun.jersey.api.Responses;
import com.sun.jersey.core.header.MediaTypes;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

@Path("/channel/{channelName}/{year}")
public class ChannelContentResource {

    private final static Logger logger = LoggerFactory.getLogger(ChannelContentResource.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private final UriInfo uriInfo;
    private final ChannelService channelService;
    private final DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime().withZoneUTC();
    private final ChannelLinkBuilder linkBuilder;

    @Inject
    public ChannelContentResource(UriInfo uriInfo, ChannelService channelService, ChannelLinkBuilder linkBuilder) {
        this.uriInfo = uriInfo;
        this.channelService = channelService;
        this.linkBuilder = linkBuilder;
    }

    @Path("/{month}/{day}/")
    @EventTimed(name = "channel.ALL.day")
    @PerChannelTimed(operationName = "day", channelNameParameter = "channelName", newName = "day")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getMinute(@PathParam("channelName") String channelName,
                              @PathParam("year") int year,
                              @PathParam("month") int month,
                              @PathParam("day") int day) {
        DateTime startTime = new DateTime(year, month, day, 0, 0, 0, 0, DateTimeZone.UTC);
        DateTime endTime = startTime.plusDays(1).minusMillis(1);
        return getResponse(channelName, startTime, endTime, TimeUtil.days(startTime.minusDays(1)), TimeUtil.days(startTime.plusDays(1)));
    }

    @Path("/{month}/{day}/{hour}")
    @EventTimed(name = "channel.ALL.hour")
    @PerChannelTimed(operationName = "hour", channelNameParameter = "channelName", newName = "hour")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getMinute(@PathParam("channelName") String channelName,
                              @PathParam("year") int year,
                              @PathParam("month") int month,
                              @PathParam("day") int day,
                              @PathParam("hour") int hour) {
        DateTime startTime = new DateTime(year, month, day, hour, 0, 0, 0, DateTimeZone.UTC);
        DateTime endTime = startTime.plusHours(1).minusMillis(1);
        return getResponse(channelName, startTime, endTime, TimeUtil.hours(startTime.minusHours(1)), TimeUtil.hours(startTime.plusHours(1)));
    }

    @Path("/{month}/{day}/{hour}/{minute}")
    @EventTimed(name = "channel.ALL.minute")
    @PerChannelTimed(operationName = "minute", channelNameParameter = "channelName", newName = "minute")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getMinute(@PathParam("channelName") String channelName,
                              @PathParam("year") int year,
                              @PathParam("month") int month,
                              @PathParam("day") int day,
                              @PathParam("hour") int hour,
                              @PathParam("minute") int minute) {
        DateTime startTime = new DateTime(year, month, day, hour, minute, 0, 0, DateTimeZone.UTC);
        DateTime endTime = startTime.plusMinutes(1).minusMillis(1);
        return getResponse(channelName, startTime, endTime, TimeUtil.minutes(startTime.minusMinutes(1)), TimeUtil.minutes(startTime.plusMinutes(1)));
    }

    @Path("/{month}/{day}/{hour}/{minute}/{second}")
    @EventTimed(name = "channel.ALL.second")
    @PerChannelTimed(operationName = "second", channelNameParameter = "channelName", newName = "second")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getSecond(@PathParam("channelName") String channelName,
                              @PathParam("year") int year,
                              @PathParam("month") int month,
                              @PathParam("day") int day,
                              @PathParam("hour") int hour,
                              @PathParam("minute") int minute,
                              @PathParam("second") int second) {
        DateTime startTime = new DateTime(year, month, day, hour, minute, second, 0, DateTimeZone.UTC);
        DateTime endTime = startTime.plusSeconds(1).minusMillis(1);
        return getResponse(channelName, startTime, endTime, TimeUtil.seconds(startTime.minusSeconds(1)), TimeUtil.seconds(startTime.plusSeconds(1)));
    }

    private Response getResponse(String channelName, DateTime startTime, DateTime endTime, String previousString, String nextString) {
        Collection<ContentKey> keys = channelService.getKeys(channelName, startTime, endTime);
        ObjectNode root = mapper.createObjectNode();
        ObjectNode links = root.putObject("_links");
        ObjectNode self = links.putObject("self");
        self.put("href", uriInfo.getRequestUri().toString());
        ObjectNode next = links.putObject("next");
        next.put("href", ChannelLinkBuilder.buildChannelString(channelName, uriInfo) + "/" + nextString);
        ObjectNode previous = links.putObject("previous");
        previous.put("href", ChannelLinkBuilder.buildChannelString(channelName, uriInfo) + "/" + previousString);
        ArrayNode ids = links.putArray("uris");
        URI channelUri = linkBuilder.buildChannelUri(channelName, uriInfo);
        for (ContentKey key : keys) {
            URI uri = linkBuilder.buildItemUri(key, channelUri);
            ids.add(uri.toString());
        }
        return Response.ok(root).build();
    }

    //todo - gfm - 1/22/14 - would be nice to have a head method, which doesn't fetch the body.

    @Path("/{month}/{day}/{hour}/{minute}/{second}/{id}")
    @GET
    @Timed(name = "all-channels.fetch")
    @EventTimed(name = "channel.ALL.get")
    @PerChannelTimed(operationName = "fetch", channelNameParameter = "channelName", newName = "get")
    @ExceptionMetered
    public Response getValue(@PathParam("channelName") String channelName, @PathParam("id") String id,
                             @HeaderParam("Accept") String accept, @HeaderParam("User") String user
    ) {
        Request request = Request.builder()
                .channel(channelName)
                .id(id)
                .user(user)
                .uri(uriInfo.getRequestUri())
                .build();
        Optional<Content> optionalResult = channelService.getValue(request);

        if (!optionalResult.isPresent()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        Content content = optionalResult.get();

        MediaType actualContentType = getContentType(content);

        if (contentTypeIsNotCompatible(accept, actualContentType)) {
            return Responses.notAcceptable().build();
        }

        Response.ResponseBuilder builder = Response.status(Response.Status.OK)
                .type(actualContentType)
                .entity(content.getData())
                .header(Headers.CREATION_DATE,
                        dateTimeFormatter.print(new DateTime(content.getMillis())));

        ChannelLinkBuilder.addOptionalHeader(Headers.USER, content.getUser(), builder);
        ChannelLinkBuilder.addOptionalHeader(Headers.LANGUAGE, content.getContentLanguage(), builder);

        //todo - gfm - 10/28/14 - clean this shit up
        URI linkUrl1 = URI.create(uriInfo.getRequestUri().resolve(".") + content.getContentKey().get().key() + "/previous");
        builder.header("Link", "<" + linkUrl1 + ">;rel=\"" + "previous" + "\"");
        URI linkUrl2 = URI.create(uriInfo.getRequestUri().resolve(".") + content.getContentKey().get().key() + "/next");
        builder.header("Link", "<" + linkUrl2 + ">;rel=\"" + "next" + "\"");
        return builder.build();
    }


    private MediaType getContentType(Content content) {
        Optional<String> contentType = content.getContentType();
        if (contentType.isPresent() && !isNullOrEmpty(contentType.get())) {
            return MediaType.valueOf(contentType.get());
        }
        return MediaType.APPLICATION_OCTET_STREAM_TYPE;
    }

    private boolean contentTypeIsNotCompatible(String acceptHeader, final MediaType actualContentType) {
        List<MediaType> acceptableContentTypes = acceptHeader != null ?
                MediaTypes.createMediaTypes(acceptHeader.split(",")) :
                MediaTypes.GENERAL_MEDIA_TYPE_LIST;

        return !Iterables.any(acceptableContentTypes, new Predicate<MediaType>() {
            @Override
            public boolean apply(MediaType input) {
                return input.isCompatible(actualContentType);
            }
        });
    }


}
