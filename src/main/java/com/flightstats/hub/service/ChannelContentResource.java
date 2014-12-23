package com.flightstats.hub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.config.metrics.EventTimed;
import com.flightstats.hub.app.config.metrics.PerChannelTimed;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Request;
import com.flightstats.hub.model.*;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteStreams;
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
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.flightstats.hub.util.TimeUtil.*;
import static com.google.common.base.Strings.isNullOrEmpty;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.SEE_OTHER;

@Path("/channel/{channelName}/{year}/{month}/{day}/")
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
                .stable(stable)
                .unit(Unit.DAYS);
        Collection<ContentKey> keys = channelService.queryByTime(builder.build());
        return getResponse(channelName, startTime.minusDays(1), startTime.plusDays(1), Unit.DAYS, keys, stable);
    }

    @Path("/{hour}")
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
                .stable(stable)
                .unit(Unit.HOURS);
        Collection<ContentKey> keys = channelService.queryByTime(builder.build());
        return getResponse(channelName, startTime.minusHours(1), startTime.plusHours(1), Unit.HOURS, keys, stable);
    }

    @Path("/{hour}/{minute}")
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
                .stable(stable)
                .unit(Unit.MINUTES)
                .location(Location.valueOf(location));
        Collection<ContentKey> keys = channelService.queryByTime(builder.build());
        return getResponse(channelName, startTime.minusMinutes(1), startTime.plusMinutes(1), Unit.MINUTES, keys, stable);
    }

    @Path("/{hour}/{minute}/{second}")
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
                .stable(stable)
                .unit(Unit.SECONDS);
        Collection<ContentKey> keys = channelService.queryByTime(builder.build());
        return getResponse(channelName, startTime.minusSeconds(1), startTime.plusSeconds(1), Unit.SECONDS, keys, stable);
    }

    @Path("/{hour}/{minute}/{second}/{millis}")
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
                .stable(stable)
                .unit(Unit.MILLIS);
        Collection<ContentKey> keys = channelService.queryByTime(builder.build());
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
        DateTime current = stable ? stable() : now();
        if (next.isBefore(current)) {
            links.putObject("next").put("href", uriInfo.getBaseUri() + "channel/" + channelName + "/" + unit.format(next) + "?stable=" + stable);
        }
        links.putObject("previous").put("href", uriInfo.getBaseUri() + "channel/" + channelName + "/" + unit.format(previous) + "?stable=" + stable);
        ArrayNode ids = links.putArray("uris");
        URI channelUri = linkBuilder.buildChannelUri(channelName, uriInfo);
        for (ContentKey key : keys) {
            URI uri = linkBuilder.buildItemUri(key, channelUri);
            ids.add(uri.toString());
        }
        return Response.ok(root).build();
    }

    private Response directionalResponse(String channelName, Collection<ContentKey> keys, int count) {
        ObjectNode root = mapper.createObjectNode();
        ObjectNode links = root.putObject("_links");
        ObjectNode self = links.putObject("self");
        self.put("href", uriInfo.getRequestUri().toString());
        List<ContentKey> list = new ArrayList<>(keys);
        if (!list.isEmpty()) {
            String baseUri = uriInfo.getBaseUri() + "channel/" + channelName + "/";
            ObjectNode next = links.putObject("next");
            next.put("href", baseUri + list.get(list.size() - 1).toUrl() + "/next/" + count);
            ObjectNode previous = links.putObject("previous");
            previous.put("href", baseUri + list.get(0).toUrl() + "/previous/" + count);
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

    @Path("/{hour}/{minute}/{second}/{millis}/{hash}")
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
        Response.ResponseBuilder builder = Response.ok(new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                ByteStreams.copy(content.getStream(), output);
            }
        });

        builder.type(actualContentType)
                .header(Headers.CREATION_DATE,
                        dateTimeFormatter.print(new DateTime(key.getMillis())));

        ChannelLinkBuilder.addOptionalHeader(Headers.USER, content.getUser(), builder);
        ChannelLinkBuilder.addOptionalHeader(Headers.LANGUAGE, content.getContentLanguage(), builder);

        builder.header("Link", "<" + URI.create(uriInfo.getRequestUri() + "/previous") + ">;rel=\"" + "previous" + "\"");
        builder.header("Link", "<" + URI.create(uriInfo.getRequestUri() + "/next") + ">;rel=\"" + "next" + "\"");
        return builder.build();
    }

    @Path("/{hour}/{minute}/{second}/{millis}/{hash}/next")
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
                            @PathParam("hash") String hash,
                            @QueryParam("stable") @DefaultValue("true") boolean stable) {
        return directional(channelName, year, month, day, hour, minute, second, millis, hash, stable, true);
    }

    @Path("/{hour}/{minute}/{second}/{millis}/{hash}/next/{count}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNextCount(@PathParam("channelName") String channelName,
                                 @PathParam("year") int year,
                                 @PathParam("month") int month,
                                 @PathParam("day") int day,
                                 @PathParam("hour") int hour,
                                 @PathParam("minute") int minute,
                                 @PathParam("second") int second,
                                 @PathParam("millis") int millis,
                                 @PathParam("hash") String hash,
                                 @PathParam("count") int count,
                                 @QueryParam("stable") @DefaultValue("true") boolean stable,
                                 @QueryParam("location") @DefaultValue("ALL") String location) {
        DateTime dateTime = new DateTime(year, month, day, hour, minute, second, millis, DateTimeZone.UTC);
        DirectionQuery query = DirectionQuery.builder()
                .channelName(channelName)
                .contentKey(new ContentKey(dateTime, hash))
                .next(true)
                .stable(stable)
                .location(Location.valueOf(location))
                .count(count).build();
        Collection<ContentKey> keys = channelService.getKeys(query);
        return directionalResponse(channelName, keys, count);
    }

    @Path("/{hour}/{minute}/{second}/{millis}/{hash}/previous")
    @GET
    public Response getPrevious(@PathParam("channelName") String channelName,
                            @PathParam("year") int year,
                            @PathParam("month") int month,
                            @PathParam("day") int day,
                            @PathParam("hour") int hour,
                            @PathParam("minute") int minute,
                            @PathParam("second") int second,
                            @PathParam("millis") int millis,
                            @PathParam("hash") String hash,
                            @QueryParam("stable") @DefaultValue("true") boolean stable) {
        return directional(channelName, year, month, day, hour, minute, second, millis, hash, stable, false);
    }

    @Path("/{hour}/{minute}/{second}/{millis}/{hash}/previous/{count}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPreviousCount(@PathParam("channelName") String channelName,
                                 @PathParam("year") int year,
                                 @PathParam("month") int month,
                                 @PathParam("day") int day,
                                 @PathParam("hour") int hour,
                                 @PathParam("minute") int minute,
                                 @PathParam("second") int second,
                                 @PathParam("millis") int millis,
                                 @PathParam("hash") String hash,
                                 @PathParam("count") int count,
                                 @QueryParam("stable") @DefaultValue("true") boolean stable,
                                 @QueryParam("location") @DefaultValue("ALL") String location) {
        DateTime dateTime = new DateTime(year, month, day, hour, minute, second, millis, DateTimeZone.UTC);
        DirectionQuery query = DirectionQuery.builder()
                .channelName(channelName)
                .contentKey(new ContentKey(dateTime, hash))
                .next(false)
                .stable(stable)
                .location(Location.valueOf(location))
                .ttlDays(channelService.getChannelConfiguration(channelName).getTtlDays())
                .count(count).build();
        Collection<ContentKey> keys = channelService.getKeys(query);
        return directionalResponse(channelName, keys, count);
    }

    private Response directional(String channelName, int year, int month, int day, int hour, int minute,
                                 int second, int millis, String hash, boolean stable, boolean next) {
        DateTime dateTime = new DateTime(year, month, day, hour, minute, second, millis, DateTimeZone.UTC);
        DirectionQuery query = DirectionQuery.builder()
                .channelName(channelName)
                .contentKey(new ContentKey(dateTime, hash))
                .next(next)
                .stable(stable)
                .ttlDays(channelService.getChannelConfiguration(channelName).getTtlDays())
                .count(1).build();
        Collection<ContentKey> keys = channelService.getKeys(query);
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
