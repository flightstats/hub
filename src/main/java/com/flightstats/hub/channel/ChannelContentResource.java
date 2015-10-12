package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Request;
import com.flightstats.hub.metrics.EventTimed;
import com.flightstats.hub.metrics.MetricsSender;
import com.flightstats.hub.model.*;
import com.flightstats.hub.rest.Headers;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.sun.jersey.api.Responses;
import com.sun.jersey.core.header.MediaTypes;
import org.apache.commons.lang3.StringUtils;
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
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.flightstats.hub.util.TimeUtil.*;
import static com.google.common.base.Strings.isNullOrEmpty;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.SEE_OTHER;

@Path("/channel/{channel}/{Y}/{M}/{D}/")
public class ChannelContentResource {

    private final static Logger logger = LoggerFactory.getLogger(ChannelContentResource.class);

    private static final DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime().withZoneUTC();

    @Inject
    private ObjectMapper mapper;
    @Inject
    private UriInfo uriInfo;
    @Inject
    private ChannelService channelService;
    @Inject
    private LinkBuilder linkBuilder;
    @Inject
    private MetricsSender sender;
    @Inject
    private TagContentResource tagContentResource;

    @Produces({MediaType.APPLICATION_JSON, "multipart/*"})
    @GET
    public Response getDay(@PathParam("channel") String channel,
                           @PathParam("Y") int year,
                           @PathParam("M") int month,
                           @PathParam("D") int day,
                           @QueryParam("location") @DefaultValue("ALL") String location,
                           @QueryParam("trace") @DefaultValue("false") boolean trace,
                           @QueryParam("stable") @DefaultValue("true") boolean stable,
                           @QueryParam("batch") @DefaultValue("false") boolean batch,
                           @QueryParam("tag") String tag) {
        DateTime startTime = new DateTime(year, month, day, 0, 0, 0, 0, DateTimeZone.UTC);
        return getTimeQueryResponse(channel, startTime, location, trace, stable, Unit.DAYS, tag, batch);
    }

    @Path("/{hour}")
    @Produces({MediaType.APPLICATION_JSON, "multipart/*"})
    @GET
    public Response getHour(@PathParam("channel") String channel,
                            @PathParam("Y") int year,
                            @PathParam("M") int month,
                            @PathParam("D") int day,
                            @PathParam("hour") int hour,
                            @QueryParam("location") @DefaultValue("ALL") String location,
                            @QueryParam("trace") @DefaultValue("false") boolean trace,
                            @QueryParam("stable") @DefaultValue("true") boolean stable,
                            @QueryParam("batch") @DefaultValue("false") boolean batch,
                            @QueryParam("tag") String tag) {
        DateTime startTime = new DateTime(year, month, day, hour, 0, 0, 0, DateTimeZone.UTC);
        return getTimeQueryResponse(channel, startTime, location, trace, stable, Unit.HOURS, tag, batch);
    }

    @Path("/{h}/{minute}")
    @Produces({MediaType.APPLICATION_JSON, "multipart/*"})
    @GET
    public Response getMinute(@PathParam("channel") String channel,
                              @PathParam("Y") int year,
                              @PathParam("M") int month,
                              @PathParam("D") int day,
                              @PathParam("h") int hour,
                              @PathParam("minute") int minute,
                              @QueryParam("location") @DefaultValue("ALL") String location,
                              @QueryParam("trace") @DefaultValue("false") boolean trace,
                              @QueryParam("stable") @DefaultValue("true") boolean stable,
                              @QueryParam("batch") @DefaultValue("false") boolean batch,
                              @QueryParam("tag") String tag) {
        DateTime startTime = new DateTime(year, month, day, hour, minute, 0, 0, DateTimeZone.UTC);
        return getTimeQueryResponse(channel, startTime, location, trace, stable, Unit.MINUTES, tag, batch);
    }

    @Path("/{h}/{m}/{second}")
    @Produces({MediaType.APPLICATION_JSON, "multipart/*"})
    @GET
    public Response getSecond(@PathParam("channel") String channel,
                              @PathParam("Y") int year,
                              @PathParam("M") int month,
                              @PathParam("D") int day,
                              @PathParam("h") int hour,
                              @PathParam("m") int minute,
                              @PathParam("second") int second,
                              @QueryParam("location") @DefaultValue("ALL") String location,
                              @QueryParam("trace") @DefaultValue("false") boolean trace,
                              @QueryParam("stable") @DefaultValue("true") boolean stable,
                              @QueryParam("batch") @DefaultValue("false") boolean batch,
                              @QueryParam("tag") String tag) {
        DateTime startTime = new DateTime(year, month, day, hour, minute, second, 0, DateTimeZone.UTC);
        return getTimeQueryResponse(channel, startTime, location, trace, stable, Unit.SECONDS, tag, batch);
    }

    public Response getTimeQueryResponse(String channel, DateTime startTime, String location, boolean trace, boolean stable,
                                         Unit unit, String tag, boolean batch) {
        if (tag != null) {
            return tagContentResource.getTimeQueryResponse(tag, startTime, location, trace, stable, unit, batch);
        }
        TimeQuery query = TimeQuery.builder()
                .channelName(channel)
                .startTime(startTime)
                .stable(stable)
                .unit(unit)
                .location(Location.valueOf(location))
                .build();
        query.trace(trace);
        Collection<ContentKey> keys = channelService.queryByTime(query);
        if (batch) {
            return MultiPartBuilder.build(keys, channel, channelService, uriInfo);
        } else {
            ObjectNode root = mapper.createObjectNode();
            ObjectNode links = root.putObject("_links");
            ObjectNode self = links.putObject("self");
            self.put("href", uriInfo.getRequestUri().toString());
            DateTime current = stable ? stable() : now();
            DateTime next = startTime.plus(unit.getDuration());
            DateTime previous = startTime.minus(unit.getDuration());
            if (next.isBefore(current)) {
                links.putObject("next").put("href", uriInfo.getBaseUri() + "channel/" + channel + "/" + unit.format(next) + "?stable=" + stable);
            }
            links.putObject("previous").put("href", uriInfo.getBaseUri() + "channel/" + channel + "/" + unit.format(previous) + "?stable=" + stable);
            ArrayNode ids = links.putArray("uris");
            URI channelUri = LinkBuilder.buildChannelUri(channel, uriInfo);
            for (ContentKey key : keys) {
                URI uri = LinkBuilder.buildItemUri(key, channelUri);
                ids.add(uri.toString());
            }
            query.getTraces().output(root);
            return Response.ok(root).build();
        }
    }

    @Path("/{h}/{m}/{s}/{ms}/{hash}")
    @GET
    @EventTimed(name = "channel.ALL.get")
    public Response getValue(@PathParam("channel") String channel,
                             @PathParam("Y") int year,
                             @PathParam("M") int month,
                             @PathParam("D") int day,
                             @PathParam("h") int hour,
                             @PathParam("m") int minute,
                             @PathParam("s") int second,
                             @PathParam("ms") int millis,
                             @PathParam("hash") String hash,
                             @HeaderParam("Accept") String accept
    ) {
        long start = System.currentTimeMillis();
        ContentKey key = new ContentKey(year, month, day, hour, minute, second, millis, hash);
        Request request = Request.builder()
                .channel(channel)
                .key(key)
                .uri(uriInfo.getRequestUri())
                .build();
        Optional<Content> optionalResult = channelService.getValue(request);

        if (!optionalResult.isPresent()) {
            logger.warn("404 content not found {} {}", channel, key);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        Content content = optionalResult.get();

        MediaType actualContentType = getContentType(content);

        if (contentTypeIsNotCompatible(accept, actualContentType)) {
            return Responses.notAcceptable().build();
        }
        Response.ResponseBuilder builder = Response.ok((StreamingOutput) output -> ByteStreams.copy(content.getStream(), output));

        builder.type(actualContentType)
                .header(Headers.CREATION_DATE,
                        dateTimeFormatter.print(new DateTime(key.getMillis())));

        LinkBuilder.addOptionalHeader(Headers.LANGUAGE, content.getContentLanguage(), builder);
        builder.header("Link", "<" + uriInfo.getRequestUriBuilder().path("previous").build() + ">;rel=\"" + "previous" + "\"");
        builder.header("Link", "<" + uriInfo.getRequestUriBuilder().path("next").build() + ">;rel=\"" + "next" + "\"");
        sender.send("channel." + channel + ".get", System.currentTimeMillis() - start);
        return builder.build();
    }

    @Path("/{h}/{m}/{s}/{ms}/{hash}/{direction : [n|p].*}")
    @GET
    public Response getDirection(@PathParam("channel") String channel,
                                 @PathParam("Y") int year,
                                 @PathParam("M") int month,
                                 @PathParam("D") int day,
                                 @PathParam("h") int hour,
                                 @PathParam("m") int minute,
                                 @PathParam("s") int second,
                                 @PathParam("ms") int millis,
                                 @PathParam("hash") String hash,
                                 @PathParam("direction") String direction,
                                 @QueryParam("stable") @DefaultValue("true") boolean stable,
                                 @QueryParam("tag") String tag) {
        ContentKey contentKey = new ContentKey(year, month, day, hour, minute, second, millis, hash);
        boolean next = direction.startsWith("n");
        if (null != tag) {
            return tagContentResource.adjacent(tag, contentKey, stable, next);
        }
        DirectionQuery query = DirectionQuery.builder()
                .channelName(channel)
                .contentKey(contentKey)
                .next(next)
                .stable(stable)
                .count(1).build();
        query.trace(false);
        Collection<ContentKey> keys = channelService.getKeys(query);
        if (keys.isEmpty()) {
            return Response.status(NOT_FOUND).build();
        }
        Response.ResponseBuilder builder = Response.status(SEE_OTHER);
        String channelUri = uriInfo.getBaseUri() + "channel/" + channel;
        ContentKey foundKey = keys.iterator().next();
        URI uri = URI.create(channelUri + "/" + foundKey.toUrl());
        builder.location(uri);
        return builder.build();
    }

    @Path("/{h}/{m}/{s}/{ms}/{hash}/{direction : [n|p].*}/{count}")
    @GET
    @Produces({MediaType.APPLICATION_JSON, "multipart/*"})
    public Response getDirectionCount(@PathParam("channel") String channel,
                                      @PathParam("Y") int year,
                                      @PathParam("M") int month,
                                      @PathParam("D") int day,
                                      @PathParam("h") int hour,
                                      @PathParam("m") int minute,
                                      @PathParam("s") int second,
                                      @PathParam("ms") int millis,
                                      @PathParam("hash") String hash,
                                      @PathParam("direction") String direction,
                                      @PathParam("count") int count,
                                      @QueryParam("stable") @DefaultValue("true") boolean stable,
                                      @QueryParam("trace") @DefaultValue("false") boolean trace,
                                      @QueryParam("location") @DefaultValue("ALL") String location,
                                      @QueryParam("batch") @DefaultValue("false") boolean batch,
                                      @QueryParam("tag") String tag) {
        ContentKey key = new ContentKey(year, month, day, hour, minute, second, millis, hash);
        boolean next = direction.startsWith("n");
        if (null != tag) {
            return tagContentResource.adjacentCount(tag, count, stable, trace, location, next, key, batch);
        }
        DirectionQuery query = DirectionQuery.builder()
                .channelName(channel)
                .contentKey(key)
                .next(next)
                .stable(stable)
                .location(Location.valueOf(location))
                .count(count)
                .build();
        query.trace(trace);
        Collection<ContentKey> keys = channelService.getKeys(query);
        if (batch) {
            return MultiPartBuilder.build(keys, channel, channelService, uriInfo);
        } else {
            return LinkBuilder.directionalResponse(channel, keys, count, query, mapper, uriInfo, true);
        }
    }


    public static MediaType getContentType(Content content) {
        Optional<String> contentType = content.getContentType();
        if (contentType.isPresent() && !isNullOrEmpty(contentType.get())) {
            return MediaType.valueOf(contentType.get());
        }
        return MediaType.APPLICATION_OCTET_STREAM_TYPE;
    }

    static boolean contentTypeIsNotCompatible(String acceptHeader, final MediaType actualContentType) {
        List<MediaType> acceptableContentTypes;
        if (StringUtils.isBlank(acceptHeader)) {
            acceptableContentTypes = MediaTypes.GENERAL_MEDIA_TYPE_LIST;
        } else {
            acceptableContentTypes = new ArrayList<>();
            String[] types = acceptHeader.split(",");
            for (String type : types) {
                acceptableContentTypes.addAll(getMediaTypes(type));
            }
        }

        return !Iterables.any(acceptableContentTypes, input -> input.isCompatible(actualContentType));
    }

    private static List<MediaType> getMediaTypes(String type) {
        try {
            return MediaTypes.createMediaTypes(new String[]{type});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }


}
