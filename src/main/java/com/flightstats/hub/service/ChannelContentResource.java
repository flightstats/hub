package com.flightstats.hub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.config.metrics.EventTimed;
import com.flightstats.hub.app.config.metrics.PerChannelTimed;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Request;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.TimeQuery;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.flightstats.hub.util.TimeUtil.Unit;
import static com.google.common.base.Strings.isNullOrEmpty;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.SEE_OTHER;

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

    //todo - gfm - 12/12/14 - what is the proper response for a time request in the future?
    //404 ?
    //307 ?

    @Path("/{month}/{day}/")
    @EventTimed(name = "channel.ALL.day")
    @PerChannelTimed(operationName = "day", channelNameParameter = "channelName")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getDay(@PathParam("channelName") String channelName,
                              @PathParam("year") int year,
                              @PathParam("month") int month,
                              @PathParam("day") int day,
                              @QueryParam("stable") @DefaultValue("true") boolean stable) {
        DateTime startTime = new DateTime(year, month, day, 0, 0, 0, 0, DateTimeZone.UTC);
        TimeQuery.TimeQueryBuilder builder = TimeQuery.builder()
                .channelName(channelName)
                .startTime(startTime)
                .unit(Unit.DAYS);
        Collection<ContentKey> keys = channelService.queryByTime(builder.build(), stable);
        return getResponse(channelName, startTime.minusDays(1), startTime.plusDays(1), Unit.DAYS, keys, stable);
    }

    @Path("/{month}/{day}/{hour}")
    @EventTimed(name = "channel.ALL.hour")
    @PerChannelTimed(operationName = "hour", channelNameParameter = "channelName")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getHour(@PathParam("channelName") String channelName,
                              @PathParam("year") int year,
                              @PathParam("month") int month,
                              @PathParam("day") int day,
                              @PathParam("hour") int hour,
                              @QueryParam("stable") @DefaultValue("true") boolean stable) {
        DateTime startTime = new DateTime(year, month, day, hour, 0, 0, 0, DateTimeZone.UTC);
        TimeQuery.TimeQueryBuilder builder = TimeQuery.builder()
                .channelName(channelName)
                .startTime(startTime)
                .unit(Unit.HOURS);
        Collection<ContentKey> keys = channelService.queryByTime(builder.build(), stable);
        return getResponse(channelName, startTime.minusHours(1), startTime.plusHours(1), Unit.HOURS, keys, stable);
    }

    @Path("/{month}/{day}/{hour}/{minute}")
    @EventTimed(name = "channel.ALL.minute")
    @PerChannelTimed(operationName = "minute", channelNameParameter = "channelName")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getMinute(@PathParam("channelName") String channelName,
                              @PathParam("year") int year,
                              @PathParam("month") int month,
                              @PathParam("day") int day,
                              @PathParam("hour") int hour,
                              @PathParam("minute") int minute,
                              @QueryParam("location") @DefaultValue("ALL") String location,
                              @QueryParam("stable") @DefaultValue("true") boolean stable) {
        DateTime startTime = new DateTime(year, month, day, hour, minute, 0, 0, DateTimeZone.UTC);
        TimeQuery.TimeQueryBuilder builder = TimeQuery.builder()
                .channelName(channelName)
                .startTime(startTime)
                .unit(Unit.MINUTES)
                .location(TimeQuery.Location.valueOf(location));
        Collection<ContentKey> keys = channelService.queryByTime(builder.build(), stable);
        return getResponse(channelName, startTime.minusMinutes(1), startTime.plusMinutes(1), Unit.MINUTES, keys, stable);
    }

    @Path("/{month}/{day}/{hour}/{minute}/{second}")
    @EventTimed(name = "channel.ALL.second")
    @PerChannelTimed(operationName = "second", channelNameParameter = "channelName")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getSecond(@PathParam("channelName") String channelName,
                              @PathParam("year") int year,
                              @PathParam("month") int month,
                              @PathParam("day") int day,
                              @PathParam("hour") int hour,
                              @PathParam("minute") int minute,
                              @PathParam("second") int second,
                              @QueryParam("stable") @DefaultValue("true") boolean stable) {
        DateTime startTime = new DateTime(year, month, day, hour, minute, second, 0, DateTimeZone.UTC);
        TimeQuery.TimeQueryBuilder builder = TimeQuery.builder()
                .channelName(channelName)
                .startTime(startTime)
                .unit(Unit.SECONDS);
        Collection<ContentKey> keys = channelService.queryByTime(builder.build(), stable);
        return getResponse(channelName, startTime.minusSeconds(1), startTime.plusSeconds(1), Unit.SECONDS, keys, stable);
    }

    @Path("/{month}/{day}/{hour}/{minute}/{second}/{millis}")
    @EventTimed(name = "channel.ALL.millis")
    @PerChannelTimed(operationName = "millis", channelNameParameter = "channelName")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getMillis(@PathParam("channelName") String channelName,
                              @PathParam("year") int year,
                              @PathParam("month") int month,
                              @PathParam("day") int day,
                              @PathParam("hour") int hour,
                              @PathParam("minute") int minute,
                              @PathParam("second") int second,
                              @PathParam("millis") int millis,
                              @QueryParam("stable") @DefaultValue("true") boolean stable) {
        DateTime startTime = new DateTime(year, month, day, hour, minute, second, millis, DateTimeZone.UTC);
        TimeQuery.TimeQueryBuilder builder = TimeQuery.builder()
                .channelName(channelName)
                .startTime(startTime)
                .unit(Unit.MILLIS);
        Collection<ContentKey> keys = channelService.queryByTime(builder.build(), stable);
        return getResponse(channelName, startTime.minusMillis(1), startTime.plusMillis(1), Unit.MILLIS , keys, stable);
    }

    private Response getResponse(String channelName,
                                 DateTime previous,
                                 DateTime next,
                                 Unit unit,
                                 Collection<ContentKey> keys,
                                 boolean stable) {
        ObjectNode root = mapper.createObjectNode();
        ObjectNode links = root.putObject("_links");
        ObjectNode self = links.putObject("self");
        self.put("href", uriInfo.getRequestUri().toString());
        //todo - gfm - 12/12/14 - limit links to the future?
        links.putObject("next").put("href", uriInfo.getBaseUri() + "channel/" + channelName + "/" + unit.format(next) + "?stable=" + stable);
        links.putObject("previous").put("href", uriInfo.getBaseUri() + "channel/" + channelName + "/" + unit.format(previous) + "?stable=" + stable);
        ArrayNode ids = links.putArray("uris");
        URI channelUri = linkBuilder.buildChannelUri(channelName, uriInfo);
        for (ContentKey key : keys) {
            URI uri = linkBuilder.buildItemUri(key, channelUri);
            ids.add(uri.toString());
        }
        return Response.ok(root).build();
    }

    private Response getResponse(String channelName, String previousString, String nextString, Collection<ContentKey> keys) {
        ObjectNode root = mapper.createObjectNode();
        ObjectNode links = root.putObject("_links");
        ObjectNode self = links.putObject("self");
        self.put("href", uriInfo.getRequestUri().toString());
        if (nextString != null) {
            ObjectNode next = links.putObject("next");
            next.put("href", uriInfo.getBaseUri() + "channel/" + channelName + "/" + nextString);
        }
        if (null != previousString) {
            ObjectNode previous = links.putObject("previous");
            previous.put("href", uriInfo.getBaseUri() + "channel/" + channelName + "/" + previousString);
        }
        ArrayNode ids = links.putArray("uris");
        URI channelUri = linkBuilder.buildChannelUri(channelName, uriInfo);
        for (ContentKey key : keys) {
            URI uri = linkBuilder.buildItemUri(key, channelUri);
            ids.add(uri.toString());
        }
        return Response.ok(root).build();
    }

    //todo - gfm - 1/22/14 - would be nice to have a head method, which doesn't fetch the body.

    @Path("/{month}/{day}/{hour}/{minute}/{second}/{millis}/{hash}")
    @GET
    @EventTimed(name = "channel.ALL.get")
    @PerChannelTimed(operationName = "fetch", channelNameParameter = "channelName")
    public Response getValue(@PathParam("channelName") String channelName, @PathParam("year") int year,
                             @PathParam("month") int month,
                             @PathParam("day") int day,
                             @PathParam("hour") int hour,
                             @PathParam("minute") int minute,
                             @PathParam("second") int second,
                             @PathParam("millis") int millis,
                             @PathParam("hash") String hash,
                             @HeaderParam("Accept") String accept, @HeaderParam("User") String user
    ) {
        DateTime dateTime = new DateTime(year, month, day, hour, minute, second, millis, DateTimeZone.UTC);
        ContentKey key = new ContentKey(dateTime, hash);
        Request request = Request.builder()
                .channel(channelName)
                .key(key)
                .user(user)
                .uri(uriInfo.getRequestUri())
                .build();
        Optional<Content> optionalResult = channelService.getValue(request);

        if (!optionalResult.isPresent()) {
            logger.warn("404 content not found {} {}", channelName, key);
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
                        dateTimeFormatter.print(new DateTime(key.getMillis())));

        ChannelLinkBuilder.addOptionalHeader(Headers.USER, content.getUser(), builder);
        ChannelLinkBuilder.addOptionalHeader(Headers.LANGUAGE, content.getContentLanguage(), builder);

        builder.header("Link", "<" + URI.create(uriInfo.getRequestUri() + "/previous") + ">;rel=\"" + "previous" + "\"");
        builder.header("Link", "<" + URI.create(uriInfo.getRequestUri() + "/next") + ">;rel=\"" + "next" + "\"");
        return builder.build();
    }

    //todo - gfm - 11/5/14 - next & previous links with stability

    @Path("/{month}/{day}/{hour}/{minute}/{second}/{millis}/{hash}/next")
    @GET
    //todo - gfm - 11/5/14 - timing?
    public Response getNext(@PathParam("channelName") String channelName,
                            @PathParam("year") int year,
                            @PathParam("month") int month,
                            @PathParam("day") int day,
                            @PathParam("hour") int hour,
                            @PathParam("minute") int minute,
                            @PathParam("second") int second,
                            @PathParam("millis") int millis,
                            @PathParam("hash") String hash) {
        //todo - gfm - 11/5/14 - more int test
        DateTime dateTime = new DateTime(year, month, day, hour, minute, second, millis, DateTimeZone.UTC);
        ContentKey key = new ContentKey(dateTime, hash);
        Collection<ContentKey> keys = channelService.getKeys(channelName, key, 1);
        if (keys.isEmpty()) {
            return Response.status(NOT_FOUND).build();
        }
        Response.ResponseBuilder builder = Response.status(SEE_OTHER);
        String channelUri = uriInfo.getBaseUri() + "channel/" + channelName;
        ContentKey foundKey = keys.iterator().next();
        URI uri = URI.create(channelUri + "/" + foundKey.toUrl());
        builder.location(uri);
        return builder.build();
    }

    @Path("/{month}/{day}/{hour}/{minute}/{second}/{millis}/{hash}/next/{count}")
    @GET
    //todo - gfm - 11/5/14 - timing?
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNextCount(@PathParam("channelName") String channelName,
                                 @PathParam("year") int year,
                                 @PathParam("month") int month,
                                 @PathParam("day") int day,
                                 @PathParam("hour") int hour,
                                 @PathParam("minute") int minute,
                                 @PathParam("second") int second,
                                 @PathParam("millis") int millis,
                                 @PathParam("hash") String hash, @PathParam("count") int count) {
        //todo - gfm - 11/5/14 - int test
        DateTime dateTime = new DateTime(year, month, day, hour, minute, second, millis, DateTimeZone.UTC);
        ContentKey key = new ContentKey(dateTime, hash);
        Collection<ContentKey> keys = channelService.getKeys(channelName, key, count);
        List<ContentKey> list = new ArrayList<>(keys);
        String nextString = null;
        if (!list.isEmpty()) {
            ContentKey contentKey = list.get(list.size() - 1);
            nextString = contentKey.toUrl() + "/next/" + count;
        }
        return getResponse(channelName, null, nextString, keys);
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
