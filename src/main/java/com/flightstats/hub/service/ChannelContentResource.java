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
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getDay(@PathParam("channelName") String channelName,
                              @PathParam("year") int year,
                              @PathParam("month") int month,
                              @PathParam("day") int day,
                              @QueryParam("location") @DefaultValue("ALL") String location,
                              @QueryParam("trace") @DefaultValue("false") boolean trace,
                              @QueryParam("stable") @DefaultValue("true") boolean stable) {
        DateTime startTime = new DateTime(year, month, day, 0, 0, 0, 0, DateTimeZone.UTC);
        return getResponse(channelName, startTime, location, trace, stable, Unit.DAYS);
    }

    @Path("/{hour}")
    @EventTimed(name = "channel.ALL.hour")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getHour(@PathParam("channelName") String channelName,
                              @PathParam("year") int year,
                              @PathParam("month") int month,
                              @PathParam("day") int day,
                              @PathParam("hour") int hour,
                              @QueryParam("location") @DefaultValue("ALL") String location,
                              @QueryParam("trace") @DefaultValue("false") boolean trace,
                              @QueryParam("stable") @DefaultValue("true") boolean stable) {
        DateTime startTime = new DateTime(year, month, day, hour, 0, 0, 0, DateTimeZone.UTC);
        return getResponse(channelName, startTime, location, trace, stable, Unit.HOURS);
    }

    @Path("/{hour}/{minute}")
    @EventTimed(name = "channel.ALL.minute")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getMinute(@PathParam("channelName") String channelName,
                              @PathParam("year") int year,
                              @PathParam("month") int month,
                              @PathParam("day") int day,
                              @PathParam("hour") int hour,
                              @PathParam("minute") int minute,
                              @QueryParam("location") @DefaultValue("ALL") String location,
                              @QueryParam("trace") @DefaultValue("false") boolean trace,
                              @QueryParam("stable") @DefaultValue("true") boolean stable) {
        DateTime startTime = new DateTime(year, month, day, hour, minute, 0, 0, DateTimeZone.UTC);
        return getResponse(channelName, startTime, location, trace, stable, Unit.MINUTES);
    }

    @Path("/{hour}/{minute}/{second}")
    @EventTimed(name = "channel.ALL.second")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getSecond(@PathParam("channelName") String channelName,
                              @PathParam("year") int year,
                              @PathParam("month") int month,
                              @PathParam("day") int day,
                              @PathParam("hour") int hour,
                              @PathParam("minute") int minute,
                              @PathParam("second") int second,
                              @QueryParam("location") @DefaultValue("ALL") String location,
                              @QueryParam("trace") @DefaultValue("false") boolean trace,
                              @QueryParam("stable") @DefaultValue("true") boolean stable) {
        DateTime startTime = new DateTime(year, month, day, hour, minute, second, 0, DateTimeZone.UTC);
        return getResponse(channelName, startTime, location, trace, stable, Unit.SECONDS);
    }

    public Response getResponse(String channelName, DateTime startTime, String location, boolean trace, boolean stable,
                                Unit unit) {
        TimeQuery query = TimeQuery.builder()
                .channelName(channelName)
                .startTime(startTime)
                .stable(stable)
                .unit(unit)
                .location(Location.valueOf(location))
                .build();
        query.trace(trace);
        Collection<ContentKey> keys = channelService.queryByTime(query);
        ObjectNode root = mapper.createObjectNode();
        ObjectNode links = root.putObject("_links");
        ObjectNode self = links.putObject("self");
        self.put("href", uriInfo.getRequestUri().toString());
        DateTime current = stable ? stable() : now();
        DateTime next = startTime.plus(unit.getDuration());
        DateTime previous = startTime.minus(unit.getDuration());
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
        query.getTraces().output(root);
        return Response.ok(root).build();
    }

    private Response directionalResponse(String channelName, Collection<ContentKey> keys, int count, DirectionQuery query) {
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
        query.getTraces().output(root);
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
                                 @QueryParam("trace") @DefaultValue("false") boolean trace,
                                 @QueryParam("location") @DefaultValue("ALL") String location) {
        DateTime dateTime = new DateTime(year, month, day, hour, minute, second, millis, DateTimeZone.UTC);
        DirectionQuery query = DirectionQuery.builder()
                .channelName(channelName)
                .contentKey(new ContentKey(dateTime, hash))
                .next(true)
                .stable(stable)
                .location(Location.valueOf(location))
                .count(count).build();
        query.trace(trace);
        Collection<ContentKey> keys = channelService.getKeys(query);
        return directionalResponse(channelName, keys, count, query);
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
                                 @QueryParam("trace") @DefaultValue("false") boolean trace,
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
        query.trace(trace);
        Collection<ContentKey> keys = channelService.getKeys(query);
        return directionalResponse(channelName, keys, count, query);
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
        query.trace(false);
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
