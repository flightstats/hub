package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Request;
import com.flightstats.hub.dao.TagService;
import com.flightstats.hub.metrics.EventTimed;
import com.flightstats.hub.metrics.MetricsSender;
import com.flightstats.hub.model.*;
import com.flightstats.hub.rest.Headers;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.sun.jersey.api.Responses;
import com.sun.jersey.core.header.MediaTypes;
import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;
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

    private final DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime().withZoneUTC();

    @Inject
    private ObjectMapper mapper;
    @Inject
    private UriInfo uriInfo;
    @Inject
    private ChannelService channelService;
    @Inject
    private TagService tagService;
    @Inject
    private LinkBuilder linkBuilder;
    @Inject
    MetricsSender sender;

    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getDay(@PathParam("channel") String channel,
                           @PathParam("Y") int year,
                           @PathParam("M") int month,
                           @PathParam("D") int day,
                           @QueryParam("location") @DefaultValue("ALL") String location,
                           @QueryParam("trace") @DefaultValue("false") boolean trace,
                           @QueryParam("stable") @DefaultValue("true") boolean stable) {
        DateTime startTime = new DateTime(year, month, day, 0, 0, 0, 0, DateTimeZone.UTC);
        return getTimeQueryResponse(channel, startTime, location, trace, stable, Unit.DAYS);
    }

    @Path("/{hour}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getHour(@PathParam("channel") String channel,
                            @PathParam("Y") int year,
                            @PathParam("M") int month,
                            @PathParam("D") int day,
                            @PathParam("hour") int hour,
                            @QueryParam("location") @DefaultValue("ALL") String location,
                            @QueryParam("trace") @DefaultValue("false") boolean trace,
                            @QueryParam("stable") @DefaultValue("true") boolean stable) {
        DateTime startTime = new DateTime(year, month, day, hour, 0, 0, 0, DateTimeZone.UTC);
        return getTimeQueryResponse(channel, startTime, location, trace, stable, Unit.HOURS);
    }

    @Path("/{h}/{minute}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getMinute(@PathParam("channel") String channel,
                              @PathParam("Y") int year,
                              @PathParam("M") int month,
                              @PathParam("D") int day,
                              @PathParam("h") int hour,
                              @PathParam("minute") int minute,
                              @QueryParam("location") @DefaultValue("ALL") String location,
                              @QueryParam("trace") @DefaultValue("false") boolean trace,
                              @QueryParam("stable") @DefaultValue("true") boolean stable) {
        DateTime startTime = new DateTime(year, month, day, hour, minute, 0, 0, DateTimeZone.UTC);
        return getTimeQueryResponse(channel, startTime, location, trace, stable, Unit.MINUTES);
    }

    @Path("/{h}/{m}/{second}")
    @Produces(MediaType.APPLICATION_JSON)
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
                              @QueryParam("stable") @DefaultValue("true") boolean stable) {
        DateTime startTime = new DateTime(year, month, day, hour, minute, second, 0, DateTimeZone.UTC);
        return getTimeQueryResponse(channel, startTime, location, trace, stable, Unit.SECONDS);
    }

    public Response getTimeQueryResponse(String channelName, DateTime startTime, String location, boolean trace, boolean stable,
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
        URI channelUri = LinkBuilder.buildChannelUri(channelName, uriInfo);
        for (ContentKey key : keys) {
            URI uri = LinkBuilder.buildItemUri(key, channelUri);
            ids.add(uri.toString());
        }
        query.getTraces().output(root);
        return Response.ok(root).build();
    }

    @Path("/{h}/{m}/{s}/{ms}/{hash}")
    @GET
    @EventTimed(name = "channel.ALL.get")
    public Response getValue(@PathParam("channel") String channel, @PathParam("Y") int year,
                             @PathParam("M") int month,
                             @PathParam("D") int day,
                             @PathParam("h") int hour,
                             @PathParam("m") int minute,
                             @PathParam("s") int second,
                             @PathParam("ms") int millis,
                             @PathParam("hash") String hash,
                             @HeaderParam("Accept") String accept, @HeaderParam("User") String user
    ) {
        long start = System.currentTimeMillis();
        ContentKey key = new ContentKey(year, month, day, hour, minute, second, millis, hash);
        Request request = Request.builder()
                .channel(channel)
                .key(key)
                .user(user)
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

        LinkBuilder.addOptionalHeader(Headers.USER, content.getUser(), builder);
        LinkBuilder.addOptionalHeader(Headers.LANGUAGE, content.getContentLanguage(), builder);
        builder.header("Link", "<" + uriInfo.getRequestUriBuilder().path("previous").build() + ">;rel=\"" + "previous" + "\"");
        builder.header("Link", "<" + uriInfo.getRequestUriBuilder().path("next").build() + ">;rel=\"" + "next" + "\"");
        sender.send("channel." + channel + ".get", System.currentTimeMillis() - start);
        return builder.build();
    }

    @Path("/{h}/{m}/{s}/{ms}/{hash}/next")
    @GET
    public Response getNext(@PathParam("channel") String channel,
                            @PathParam("Y") int year,
                            @PathParam("M") int month,
                            @PathParam("D") int day,
                            @PathParam("h") int hour,
                            @PathParam("m") int minute,
                            @PathParam("s") int second,
                            @PathParam("ms") int millis,
                            @PathParam("hash") String hash,
                            @QueryParam("stable") @DefaultValue("true") boolean stable,
                            @QueryParam("tag") String tag) {
        ContentKey contentKey = new ContentKey(year, month, day, hour, minute, second, millis, hash);
        return adjacent(channel, contentKey, stable, true, tag);
    }

    @Path("/{h}/{m}/{s}/{ms}/{hash}/previous")
    @GET
    public Response getPrevious(@PathParam("channel") String channel,
                                @PathParam("Y") int year,
                                @PathParam("M") int month,
                                @PathParam("D") int day,
                                @PathParam("h") int hour,
                                @PathParam("m") int minute,
                                @PathParam("s") int second,
                                @PathParam("ms") int millis,
                                @PathParam("hash") String hash,
                                @QueryParam("stable") @DefaultValue("true") boolean stable,
                                @QueryParam("tag") String tag) {
        ContentKey contentKey = new ContentKey(year, month, day, hour, minute, second, millis, hash);
        return adjacent(channel, contentKey, stable, false, tag);
    }

    private Response adjacent(String channel, ContentKey contentKey, boolean stable, boolean next, String tag) {
        if (null != tag) {
            return TagContentResource.adjacent(tag, contentKey, stable, next, tagService, uriInfo);
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

    @Path("/{h}/{m}/{s}/{ms}/{hash}/next/{count}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNextCount(@PathParam("channel") String channel,
                                 @PathParam("Y") int year,
                                 @PathParam("M") int month,
                                 @PathParam("D") int day,
                                 @PathParam("h") int hour,
                                 @PathParam("m") int minute,
                                 @PathParam("s") int second,
                                 @PathParam("ms") int millis,
                                 @PathParam("hash") String hash,
                                 @PathParam("count") int count,
                                 @QueryParam("stable") @DefaultValue("true") boolean stable,
                                 @QueryParam("trace") @DefaultValue("false") boolean trace,
                                 @QueryParam("location") @DefaultValue("ALL") String location) {
        ContentKey key = new ContentKey(year, month, day, hour, minute, second, millis, hash);
        return adjacentCount(channel, count, stable, trace, location, true, key);
    }

    @Path("/{h}/{m}/{s}/{ms}/{hash}/previous/{count}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPreviousCount(@PathParam("channel") String channel,
                                     @PathParam("Y") int year,
                                     @PathParam("M") int month,
                                     @PathParam("D") int day,
                                     @PathParam("h") int hour,
                                     @PathParam("m") int minute,
                                     @PathParam("s") int second,
                                     @PathParam("ms") int millis,
                                     @PathParam("hash") String hash,
                                     @PathParam("count") int count,
                                     @QueryParam("stable") @DefaultValue("true") boolean stable,
                                     @QueryParam("trace") @DefaultValue("false") boolean trace,
                                     @QueryParam("location") @DefaultValue("ALL") String location) {
        ContentKey key = new ContentKey(year, month, day, hour, minute, second, millis, hash);
        return adjacentCount(channel, count, stable, trace, location, false, key);
    }

    private Response adjacentCount(String channel, int count, boolean stable, boolean trace, String location, boolean next, ContentKey contentKey) {
        DirectionQuery query = DirectionQuery.builder()
                .channelName(channel)
                .contentKey(contentKey)
                .next(next)
                .stable(stable)
                .location(Location.valueOf(location))
                .count(count).build();
        query.trace(trace);
        Collection<ContentKey> keys = channelService.getKeys(query);
        return LinkBuilder.directionalResponse(channel, keys, count, query, mapper, uriInfo, true);
    }

    private MediaType getContentType(Content content) {
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

        return !Iterables.any(acceptableContentTypes, new Predicate<MediaType>() {
            @Override
            public boolean apply(MediaType input) {
                return input.isCompatible(actualContentType);
            }
        });
    }

    private static List<MediaType> getMediaTypes(String type) {
        try {
            return MediaTypes.createMediaTypes(new String[]{type});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }


}
