package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Request;
import com.flightstats.hub.events.ContentOutput;
import com.flightstats.hub.events.EventsService;
import com.flightstats.hub.exception.ContentTooLargeException;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.DataDog;
import com.flightstats.hub.metrics.MetricsSender;
import com.flightstats.hub.metrics.NewRelicIgnoreTransaction;
import com.flightstats.hub.model.*;
import com.flightstats.hub.rest.Linked;
import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import com.sun.jersey.core.header.MediaTypes;
import com.timgroup.statsd.StatsDClient;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.SseFeature;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

import static com.flightstats.hub.rest.Linked.linked;
import static com.flightstats.hub.util.TimeUtil.*;
import static com.google.common.base.Strings.isNullOrEmpty;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.SEE_OTHER;

@Path("/channel/{channel}/{Y}/{M}/{D}/")
public class ChannelContentResource {
    static final String CREATION_DATE = "Creation-Date";
    private final static StatsDClient statsd = DataDog.statsd;

    private final static Logger logger = LoggerFactory.getLogger(ChannelContentResource.class);

    @Context
    private UriInfo uriInfo;

    private TagContentResource tagContentResource = HubProvider.getInstance(TagContentResource.class);
    private ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private ChannelService channelService = HubProvider.getInstance(ChannelService.class);
    private MetricsSender sender = HubProvider.getInstance(MetricsSender.class);
    private EventsService eventsService = HubProvider.getInstance(EventsService.class);

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
        return !acceptableContentTypes.stream().anyMatch(input -> input.isCompatible(actualContentType));
    }

    private static List<MediaType> getMediaTypes(String type) {
        try {
            return MediaTypes.createMediaTypes(new String[]{type});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Produces({MediaType.APPLICATION_JSON, "multipart/*", "application/zip"})
    @GET
    public Response getDay(@PathParam("channel") String channel,
                           @PathParam("Y") int year,
                           @PathParam("M") int month,
                           @PathParam("D") int day,
                           @QueryParam("location") @DefaultValue("ALL") String location,
                           @QueryParam("trace") @DefaultValue("false") boolean trace,
                           @QueryParam("stable") @DefaultValue("true") boolean stable,
                           @QueryParam("batch") @DefaultValue("false") boolean batch,
                           @QueryParam("bulk") @DefaultValue("false") boolean bulk,
                           @QueryParam("tag") String tag,
                           @HeaderParam("Accept") String accept) {
        DateTime startTime = new DateTime(year, month, day, 0, 0, 0, 0, DateTimeZone.UTC);
        return getTimeQueryResponse(channel, startTime, location, trace, stable, Unit.DAYS, tag, bulk || batch, accept);
    }

    @Path("/{hour}")
    @Produces({MediaType.APPLICATION_JSON, "multipart/*", "application/zip"})
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
                            @QueryParam("bulk") @DefaultValue("false") boolean bulk,
                            @QueryParam("tag") String tag,
                            @HeaderParam("Accept") String accept) {
        DateTime startTime = new DateTime(year, month, day, hour, 0, 0, 0, DateTimeZone.UTC);
        return getTimeQueryResponse(channel, startTime, location, trace, stable, Unit.HOURS, tag, bulk || batch, accept);
    }

    @Path("/{h}/{minute}")
    @Produces({MediaType.APPLICATION_JSON, "multipart/*", "application/zip"})
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
                              @QueryParam("bulk") @DefaultValue("false") boolean bulk,
                              @QueryParam("tag") String tag,
                              @HeaderParam("Accept") String accept) {
        DateTime startTime = new DateTime(year, month, day, hour, minute, 0, 0, DateTimeZone.UTC);
        return getTimeQueryResponse(channel, startTime, location, trace, stable, Unit.MINUTES, tag, bulk || batch, accept);
    }

    @Path("/{h}/{m}/{second}")
    @Produces({MediaType.APPLICATION_JSON, "multipart/*", "application/zip"})
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
                              @QueryParam("bulk") @DefaultValue("false") boolean bulk,
                              @QueryParam("tag") String tag,
                              @HeaderParam("Accept") String accept) {
        DateTime startTime = new DateTime(year, month, day, hour, minute, second, 0, DateTimeZone.UTC);
        return getTimeQueryResponse(channel, startTime, location, trace, stable, Unit.SECONDS, tag, bulk || batch, accept);
    }

    private Response getTimeQueryResponse(String channel, DateTime startTime, String location, boolean trace, boolean stable,
                                          Unit unit, String tag, boolean bulk, String accept) {
        //todo - gfm - 12/15/15 - merge this with TagContentResource.getTimeQueryResponse
        if (tag != null) {
            return tagContentResource.getTimeQueryResponse(tag, startTime, location, trace, stable, unit, bulk, accept, uriInfo);
        }
        TimeQuery query = TimeQuery.builder()
                .channelName(channel)
                .startTime(startTime)
                .stable(stable)
                .unit(unit)
                .location(Location.valueOf(location))
                .build();
        SortedSet<ContentKey> keys = channelService.queryByTime(query);
        DateTime current = stable ? stable() : now();
        DateTime next = startTime.plus(unit.getDuration());
        DateTime previous = startTime.minus(unit.getDuration());
        if (bulk) {
            return BulkBuilder.build(keys, channel, channelService, uriInfo, accept, (builder) -> {
                if (next.isBefore(current)) {
                    builder.header("Link", "<" + TimeLinkUtil.getUri(channel, uriInfo, unit, next) +
                            ">;rel=\"" + "next" + "\"");
                }
                builder.header("Link", "<" + TimeLinkUtil.getUri(channel, uriInfo, unit, previous) +
                        ">;rel=\"" + "previous" + "\"");
            });
        } else {
            ObjectNode root = mapper.createObjectNode();
            ObjectNode links = root.putObject("_links");
            ObjectNode self = links.putObject("self");
            self.put("href", uriInfo.getRequestUri().toString());
            if (next.isBefore(current)) {
                links.putObject("next").put("href", TimeLinkUtil.getUri(channel, uriInfo, unit, next).toString());
            }
            links.putObject("previous").put("href", TimeLinkUtil.getUri(channel, uriInfo, unit, previous).toString());
            ArrayNode ids = links.putArray("uris");
            URI channelUri = LinkBuilder.buildChannelUri(channel, uriInfo);
            for (ContentKey key : keys) {
                URI uri = LinkBuilder.buildItemUri(key, channelUri);
                ids.add(uri.toString());
            }
            if (trace) {
                ActiveTraces.getLocal().output(root);
            }
            return Response.ok(root).build();
        }
    }

    @Path("/{h}/{m}/{s}/{ms}/{hash}")
    @GET
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
        Optional<Content> optionalResult = channelService.get(request);

        if (!optionalResult.isPresent()) {
            logger.warn("404 content not found {} {}", channel, key);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        Content content = optionalResult.get();

        MediaType actualContentType = getContentType(content);

        if (contentTypeIsNotCompatible(accept, actualContentType)) {
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        Response.ResponseBuilder builder = Response.ok((StreamingOutput) output -> ByteStreams.copy(content.getStream(), output));

        builder.type(actualContentType)
                .header(CREATION_DATE, FORMATTER.print(new DateTime(key.getMillis())));

        builder.header("Link", "<" + uriInfo.getRequestUriBuilder().path("previous").build() + ">;rel=\"" + "previous" + "\"");
        builder.header("Link", "<" + uriInfo.getRequestUriBuilder().path("next").build() + ">;rel=\"" + "next" + "\"");
        long time = System.currentTimeMillis() - start;
        sender.send("channel." + channel + ".get", time);
        statsd.time("channel", time, "channel:" + channel, "method:get");

        return builder.build();
    }

    @Path("/{h}/{m}/{s}/{ms}/{hash}/{direction:p.*}")
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
                                @PathParam("direction") String direction,
                                @QueryParam("stable") @DefaultValue("true") boolean stable,
                                @QueryParam("tag") String tag) {
        ContentKey contentKey = new ContentKey(year, month, day, hour, minute, second, millis, hash);
        return adjacent(channel, stable, tag, contentKey, direction.startsWith("n"));
    }

    @Path("/{h}/{m}/{s}/{ms}/{hash}/{direction:n.*}")
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
                            @PathParam("direction") String direction,
                            @QueryParam("stable") @DefaultValue("true") boolean stable,
                            @QueryParam("tag") String tag) {
        ContentKey contentKey = new ContentKey(year, month, day, hour, minute, second, millis, hash);
        return adjacent(channel, stable, tag, contentKey, direction.startsWith("n"));
    }

    private Response adjacent(String channel, boolean stable, String tag, ContentKey contentKey, boolean next) {
        if (null != tag) {
            return tagContentResource.adjacent(tag, contentKey, stable, next, uriInfo);
        }
        DirectionQuery query = DirectionQuery.builder()
                .channelName(channel)
                .contentKey(contentKey)
                .next(next)
                .stable(stable)
                .count(1).build();
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

    @GET
    @Path("/{h}/{m}/{s}/{ms}/{hash}/events")
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    @NewRelicIgnoreTransaction
    public EventOutput getEvents(@PathParam("channel") String channel,
                                 @PathParam("Y") int year,
                                 @PathParam("M") int month,
                                 @PathParam("D") int day,
                                 @PathParam("h") int hour,
                                 @PathParam("m") int minute,
                                 @PathParam("s") int second,
                                 @PathParam("ms") int millis,
                                 @PathParam("hash") String hash,
                                 @HeaderParam("Last-Event-ID") String lastEventId) {
        ContentKey contentKey = new ContentKey(year, month, day, hour, minute, second, millis, hash);
        ContentKey parsedKey = ContentKey.fromFullUrl(lastEventId);
        if (parsedKey != null) {
            contentKey = parsedKey;
        }
        try {
            logger.info("starting events at {} for client from {}", channel, contentKey);
            EventOutput eventOutput = new EventOutput();
            eventsService.register(new ContentOutput(channel, eventOutput, contentKey, uriInfo.getBaseUri()));
            return eventOutput;
        } catch (Exception e) {
            logger.warn("unable to get events for " + channel, e);
            throw e;
        }
    }

    @Path("/{h}/{m}/{s}/{ms}/{hash}/{direction:p.*}/{count}")
    @GET
    @Produces({MediaType.APPLICATION_JSON, "multipart/*", "application/zip"})
    public Response getPreviousCount(@PathParam("channel") String channel,
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
                                     @QueryParam("bulk") @DefaultValue("false") boolean bulk,
                                     @QueryParam("tag") String tag,
                                     @HeaderParam("Accept") String accept) {
        ContentKey key = new ContentKey(year, month, day, hour, minute, second, millis, hash);
        return adjacentCount(channel, count, stable, trace, location, batch, bulk, tag, accept, key, direction.startsWith("n"));
    }

    @Path("/{h}/{m}/{s}/{ms}/{hash}/{direction:n.*}/{count}")
    @GET
    @Produces({MediaType.APPLICATION_JSON, "multipart/*", "application/zip"})
    public Response getNextCount(@PathParam("channel") String channel,
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
                                 @QueryParam("bulk") @DefaultValue("false") boolean bulk,
                                 @QueryParam("tag") String tag,
                                 @HeaderParam("Accept") String accept) {
        ContentKey key = new ContentKey(year, month, day, hour, minute, second, millis, hash);
        return adjacentCount(channel, count, stable, trace, location, batch, bulk, tag, accept, key, direction.startsWith("n"));
    }

    private Response adjacentCount(String channel, int count, boolean stable, boolean trace, String location, boolean batch, boolean bulk, String tag, String accept, ContentKey key, boolean next) {
        if (null != tag) {
            return tagContentResource.adjacentCount(tag, count, stable, trace, location, next, key, bulk || batch, accept, uriInfo);
        }
        DirectionQuery query = DirectionQuery.builder()
                .channelName(channel)
                .contentKey(key)
                .next(next)
                .stable(stable)
                .location(Location.valueOf(location))
                .count(count)
                .build();
        SortedSet<ContentKey> keys = channelService.getKeys(query);
        if (bulk || batch) {
            return BulkBuilder.build(keys, channel, channelService, uriInfo, accept, (builder) -> {
                if (!keys.isEmpty()) {
                    builder.header("Link", "<" + LinkBuilder.getDirection("previous", channel, uriInfo, keys.first(), count) +
                            ">;rel=\"" + "previous" + "\"");
                    builder.header("Link", "<" + LinkBuilder.getDirection("next", channel, uriInfo, keys.last(), count) +
                            ">;rel=\"" + "next" + "\"");
                }
            });
        } else {
            return LinkBuilder.directionalResponse(channel, keys, count, query, mapper, uriInfo, true, trace);
        }
    }

    @Path("/{h}/{m}/{s}/{ms}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response historicalInsert(@PathParam("channel") final String channelName,
                                     @PathParam("Y") int year,
                                     @PathParam("M") int month,
                                     @PathParam("D") int day,
                                     @PathParam("h") int hour,
                                     @PathParam("m") int minute,
                                     @PathParam("s") int second,
                                     @PathParam("ms") int millis,
                                     @HeaderParam("Content-Type") String contentType,
                                     @HeaderParam("Content-Language") String contentLanguage,
                                     @HeaderParam("minuteComplete") @DefaultValue("false") boolean minuteComplete,
                                     final InputStream data) throws Exception {
        if (!channelService.channelExists(channelName)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        ContentKey key = new ContentKey(year, month, day, hour, minute, second, millis);
        Content content = Content.builder()
                .withContentKey(key)
                .withContentType(contentType)
                .withStream(data)
                .build();
        try {
            boolean success = channelService.historicalInsert(channelName, content, minuteComplete);
            if (!success) {
                return Response.status(400).entity("unable to insert historical item").build();
            }

            logger.trace("posted {}", key);
            InsertedContentKey insertionResult = new InsertedContentKey(key);
            URI payloadUri = new URI(uriInfo.getRequestUri() + "/" + key.getHash());
            Linked<InsertedContentKey> linkedResult = linked(insertionResult)
                    .withLink("channel", LinkBuilder.buildChannelUri(channelName, uriInfo))
                    .withLink("self", payloadUri)
                    .build();

            Response.ResponseBuilder builder = Response.status(Response.Status.CREATED);
            builder.entity(linkedResult);
            builder.location(payloadUri);
            return builder.build();
        } catch (InvalidRequestException e) {
            return Response.status(400).entity(e.getMessage()).build();
        } catch (ContentTooLargeException e) {
            return Response.status(413).entity(e.getMessage()).build();
        } catch (Exception e) {
            logger.warn("unable to POST to " + channelName + " key " + key, e);
            throw e;
        }
    }

    @Path("/{h}/{m}/{s}/{ms}")
    @GET
    public Response getMillis(@PathParam("channel") String channel,
                              @PathParam("Y") String year,
                              @PathParam("M") String month,
                              @PathParam("D") String day,
                              @PathParam("h") String hour,
                              @PathParam("m") String minute,
                              @PathParam("s") String second,
                              @PathParam("ms") String millis) {
        try {
            URI redirect = UriBuilder.fromUri(uriInfo.getBaseUri())
                    .path("channel")
                    .path(channel)
                    .path(year).path(month).path(day)
                    .path(hour).path(minute).path(second).build();
            return Response.seeOther(redirect).build();
        } catch (Exception e) {
            logger.warn("what!?!", e);
            return null;
        }
    }
}
