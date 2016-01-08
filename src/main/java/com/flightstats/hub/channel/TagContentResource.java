package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.dao.Request;
import com.flightstats.hub.dao.TagService;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.EventTimed;
import com.flightstats.hub.metrics.MetricsSender;
import com.flightstats.hub.model.*;
import com.flightstats.hub.rest.Headers;
import com.flightstats.hub.rest.Linked;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import static com.flightstats.hub.util.TimeUtil.Unit;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.SEE_OTHER;

@Path("/tag/{tag}")
public class TagContentResource {

    private final static Logger logger = LoggerFactory.getLogger(TagContentResource.class);

    @Context
    private UriInfo uriInfo;

    private ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private TagService tagService = HubProvider.getInstance(TagService.class);
    private MetricsSender sender = HubProvider.getInstance(MetricsSender.class);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTagLinks(@PathParam("tag") String tag) {
        Iterable<ChannelConfig> channels = tagService.getChannels(tag);
        Map<String, URI> mappedUris = new HashMap<>();
        for (ChannelConfig channelConfig : channels) {
            String channelName = channelConfig.getName();
            mappedUris.put(channelName, LinkBuilder.buildChannelUri(channelName, uriInfo));
        }
        Linked<?> result = LinkBuilder.buildLinks(mappedUris, "channels", builder -> {
            String uri = uriInfo.getRequestUri().toString();
            builder.withLink("self", uriInfo.getRequestUri())
                    .withLink("latest", uri + "/latest")
                    .withLink("earliest", uri + "/earliest")
                            //.withLink("status", uri + "/status")
                    .withLink("time", uri + "/time");

        });
        return Response.ok(result).build();
    }

    @Path("/{Y}/{M}/{D}/")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getDay(@PathParam("tag") String tag,
                           @PathParam("Y") int year,
                           @PathParam("M") int month,
                           @PathParam("D") int day,
                           @QueryParam("location") @DefaultValue("ALL") String location,
                           @QueryParam("trace") @DefaultValue("false") boolean trace,
                           @QueryParam("batch") @DefaultValue("false") boolean batch,
                           @QueryParam("bulk") @DefaultValue("false") boolean bulk,
                           @QueryParam("stable") @DefaultValue("true") boolean stable,
                           @HeaderParam("Accept") String accept) {
        DateTime startTime = new DateTime(year, month, day, 0, 0, 0, 0, DateTimeZone.UTC);
        return getTimeQueryResponse(tag, startTime, location, trace, stable, Unit.DAYS, bulk || batch, accept, uriInfo);
    }

    @Path("/{Y}/{M}/{D}/{hour}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getHour(@PathParam("tag") String tag,
                            @PathParam("Y") int year,
                            @PathParam("M") int month,
                            @PathParam("D") int day,
                            @PathParam("hour") int hour,
                            @QueryParam("location") @DefaultValue("ALL") String location,
                            @QueryParam("trace") @DefaultValue("false") boolean trace,
                            @QueryParam("batch") @DefaultValue("false") boolean batch,
                            @QueryParam("bulk") @DefaultValue("false") boolean bulk,
                            @QueryParam("stable") @DefaultValue("true") boolean stable,
                            @HeaderParam("Accept") String accept) {
        DateTime startTime = new DateTime(year, month, day, hour, 0, 0, 0, DateTimeZone.UTC);
        return getTimeQueryResponse(tag, startTime, location, trace, stable, Unit.HOURS, bulk || batch, accept, uriInfo);
    }

    @Path("/{Y}/{M}/{D}/{h}/{minute}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getMinute(@PathParam("tag") String tag,
                              @PathParam("Y") int year,
                              @PathParam("M") int month,
                              @PathParam("D") int day,
                              @PathParam("h") int hour,
                              @PathParam("minute") int minute,
                              @QueryParam("location") @DefaultValue("ALL") String location,
                              @QueryParam("trace") @DefaultValue("false") boolean trace,
                              @QueryParam("batch") @DefaultValue("false") boolean batch,
                              @QueryParam("bulk") @DefaultValue("false") boolean bulk,
                              @QueryParam("stable") @DefaultValue("true") boolean stable,
                              @HeaderParam("Accept") String accept) {
        DateTime startTime = new DateTime(year, month, day, hour, minute, 0, 0, DateTimeZone.UTC);
        return getTimeQueryResponse(tag, startTime, location, trace, stable, Unit.MINUTES, bulk || batch, accept, uriInfo);
    }

    @Path("/{Y}/{M}/{D}/{h}/{m}/{second}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getSecond(@PathParam("tag") String tag,
                              @PathParam("Y") int year,
                              @PathParam("M") int month,
                              @PathParam("D") int day,
                              @PathParam("h") int hour,
                              @PathParam("m") int minute,
                              @PathParam("second") int second,
                              @QueryParam("location") @DefaultValue("ALL") String location,
                              @QueryParam("trace") @DefaultValue("false") boolean trace,
                              @QueryParam("batch") @DefaultValue("false") boolean batch,
                              @QueryParam("bulk") @DefaultValue("false") boolean bulk,
                              @QueryParam("stable") @DefaultValue("true") boolean stable,
                              @HeaderParam("Accept") String accept) {
        DateTime startTime = new DateTime(year, month, day, hour, minute, second, 0, DateTimeZone.UTC);
        return getTimeQueryResponse(tag, startTime, location, trace, stable, Unit.SECONDS, bulk || batch, accept, uriInfo);
    }

    public Response getTimeQueryResponse(String tag, DateTime startTime, String location, boolean trace, boolean stable,
                                         Unit unit, boolean bulk, String accept, UriInfo uriInfo) {
        //todo - gfm - 12/15/15 - merge this with ChannelContentResource.getTimeQueryResponse
        TimeQuery query = TimeQuery.builder()
                .tagName(tag)
                .startTime(startTime)
                .stable(stable)
                .unit(unit)
                .location(Location.valueOf(location))
                .build();
        SortedSet<ChannelContentKey> keys = tagService.queryByTime(query);
        DateTime current = TimeUtil.time(stable);
        DateTime next = startTime.plus(unit.getDuration());
        DateTime previous = startTime.minus(unit.getDuration());
        String baseUri = uriInfo.getBaseUri() + "tag/" + tag + "/";
        if (bulk) {
            return BulkBuilder.buildTag(tag, keys, tagService.getChannelService(), uriInfo, accept, (builder) -> {
                if (next.isBefore(current)) {
                    builder.header("Link", "<" + baseUri + unit.format(next) + "?bulk=true&stable=" + stable + ">;rel=\"" + "next" + "\"");
                }
                builder.header("Link", "<" + baseUri + unit.format(previous) + "?bulk=true&stable=" + stable + ">;rel=\"" + "previous" + "\"");
            });
        }
        ObjectNode root = mapper.createObjectNode();
        ObjectNode links = root.putObject("_links");
        ObjectNode self = links.putObject("self");
        self.put("href", uriInfo.getRequestUri().toString());
        if (next.isBefore(current)) {
            links.putObject("next").put("href", baseUri + unit.format(next) + "?stable=" + stable);
        }
        links.putObject("previous").put("href", baseUri + unit.format(previous) + "?stable=" + stable);
        ArrayNode ids = links.putArray("uris");
        for (ChannelContentKey key : keys) {
            URI channelUri = LinkBuilder.buildChannelUri(key.getChannel(), uriInfo);
            URI uri = LinkBuilder.buildItemUri(key.getContentKey(), channelUri);
            ids.add(uri.toString() + "?tag=" + tag);
        }
        if (trace) {
            ActiveTraces.getLocal().output(root);
        }
        return Response.ok(root).build();
    }

    @Path("/{Y}/{M}/{D}/{h}/{m}/{s}/{ms}/{hash}")
    @GET
    @EventTimed(name = "channel.ALL.get")
    public Response getValue(@PathParam("tag") String tag,
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
                .tag(tag)
                .key(key)
                .uri(uriInfo.getRequestUri())
                .build();
        Optional<Content> optionalResult = tagService.getValue(request);

        if (!optionalResult.isPresent()) {
            logger.warn("404 content not found {} {}", tag, key);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        Content content = optionalResult.get();

        MediaType actualContentType = ChannelContentResource.getContentType(content);

        if (ChannelContentResource.contentTypeIsNotCompatible(accept, actualContentType)) {
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        Response.ResponseBuilder builder = Response.ok((StreamingOutput) output -> ByteStreams.copy(content.getStream(), output));

        builder.type(actualContentType)
                .header(Headers.CREATION_DATE,
                        HubUtils.FORMATTER.print(new DateTime(key.getMillis())));

        LinkBuilder.addOptionalHeader(Headers.LANGUAGE, content.getContentLanguage(), builder);
        builder.header("Link", "<" + uriInfo.getRequestUriBuilder().path("previous").build() + ">;rel=\"" + "previous" + "\"");
        builder.header("Link", "<" + uriInfo.getRequestUriBuilder().path("next").build() + ">;rel=\"" + "next" + "\"");
        return builder.build();
    }

    @Path("/{Y}/{M}/{D}/{h}/{m}/{s}/{ms}/{hash}/{direction : [n|p].*}")
    @GET
    public Response getDirection(@PathParam("tag") String tag,
                                 @PathParam("Y") int year,
                                 @PathParam("M") int month,
                                 @PathParam("D") int day,
                                 @PathParam("h") int hour,
                                 @PathParam("m") int minute,
                                 @PathParam("s") int second,
                                 @PathParam("ms") int millis,
                                 @PathParam("hash") String hash,
                                 @PathParam("direction") String direction,
                                 @QueryParam("stable") @DefaultValue("true") boolean stable) {
        ContentKey contentKey = new ContentKey(year, month, day, hour, minute, second, millis, hash);
        return adjacent(tag, contentKey, stable, direction.startsWith("n"), uriInfo);
    }

    public Response adjacent(String tag, ContentKey contentKey, boolean stable, boolean next, UriInfo uriInfo) {
        DirectionQuery query = DirectionQuery.builder()
                .tagName(tag)
                .contentKey(contentKey)
                .next(next)
                .stable(stable)
                .count(1).build();
        Collection<ChannelContentKey> keys = tagService.getKeys(query);
        if (keys.isEmpty()) {
            return Response.status(NOT_FOUND).build();
        }
        Response.ResponseBuilder builder = Response.status(SEE_OTHER);
        ChannelContentKey foundKey = keys.iterator().next();
        URI uri = uriInfo.getBaseUriBuilder()
                .path("channel")
                .path(foundKey.getChannel())
                .path(foundKey.getContentKey().toUrl())
                .queryParam("tag", tag)
                .queryParam("stable", stable)
                .build();
        logger.trace("returning url {}", uri);
        builder.location(uri);
        return builder.build();
    }

    @Path("/{Y}/{M}/{D}/{h}/{m}/{s}/{ms}/{hash}/{direction : [n|p].*}/{count}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDirectionCount(@PathParam("tag") String tag,
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
                                      @QueryParam("batch") @DefaultValue("false") boolean batch,
                                      @QueryParam("bulk") @DefaultValue("false") boolean bulk,
                                      @QueryParam("location") @DefaultValue("ALL") String location,
                                      @HeaderParam("Accept") String accept) {
        ContentKey key = new ContentKey(year, month, day, hour, minute, second, millis, hash);
        return adjacentCount(tag, count, stable, trace, location, direction.startsWith("n"), key, bulk || batch, accept, uriInfo);
    }

    public Response adjacentCount(String tag, int count, boolean stable, boolean trace, String location,
                                  boolean next, ContentKey contentKey, boolean bulk, String accept, UriInfo uriInfo) {
        DirectionQuery query = DirectionQuery.builder()
                .tagName(tag)
                .contentKey(contentKey)
                .next(next)
                .stable(stable)
                .location(Location.valueOf(location))
                .count(count).build();
        SortedSet<ChannelContentKey> keys = tagService.getKeys(query);
        if (bulk) {
            return BulkBuilder.buildTag(tag, keys, tagService.getChannelService(), uriInfo, accept, (builder) -> {
                String baseUri = uriInfo.getBaseUri() + "tag/" + tag + "/";
                if (!keys.isEmpty()) {
                    builder.header("Link", "<" + baseUri + keys.first().getContentKey().toUrl() + "/previous/" + count + "?bulk=true>;rel=\"" + "previous" + "\"");
                    builder.header("Link", "<" + baseUri + keys.last().getContentKey().toUrl() + "/next/" + count + "?bulk=true>;rel=\"" + "next" + "\"");
                }
            });
        }
        return LinkBuilder.directionalTagResponse(tag, keys, count, query, mapper, uriInfo, true, trace);
    }
}
