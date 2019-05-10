package com.flightstats.hub.channel;

import com.flightstats.hub.dao.ItemRequest;
import com.flightstats.hub.dao.TagService;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.Epoch;
import com.flightstats.hub.model.Location;
import com.flightstats.hub.model.Order;
import com.flightstats.hub.rest.Linked;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.io.ByteStreams;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.flightstats.hub.util.Constants.CREATION_DATE;
import static com.flightstats.hub.util.TimeUtil.Unit;

@Slf4j
@Path("/tag/{tag}")
public class TagContentResource {

    private final TagService tagService;

    @Context
    private UriInfo uriInfo;

    @Inject
    public TagContentResource(TagService tagService) {
        this.tagService = tagService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTagLinks(@PathParam("tag") String tag) {
        final Iterable<ChannelConfig> channels = tagService.getChannels(tag);
        final Map<String, URI> mappedUris = new HashMap<>();
        for (ChannelConfig channelConfig : channels) {
            final String channelName = channelConfig.getDisplayName();
            mappedUris.put(channelName, LinkBuilder.buildChannelUri(channelName, uriInfo));
        }
        final Linked<?> result = LinkBuilder.buildLinks(mappedUris, "channels", builder ->
                builder.withLink("self", uriInfo.getRequestUri())
                        .withRelativeLink("latest", uriInfo)
                        .withRelativeLink("earliest", uriInfo)
                        .withRelativeLink("time", uriInfo));

        return Response.ok(result).build();
    }

    @Path("/{Y}/{M}/{D}/")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getDay(@PathParam("tag") String tag,
                           @PathParam("Y") int year,
                           @PathParam("M") int month,
                           @PathParam("D") int day,
                           @QueryParam("location") @DefaultValue(Location.DEFAULT) String location,
                           @QueryParam("epoch") @DefaultValue(Epoch.DEFAULT) String epoch,
                           @QueryParam("trace") @DefaultValue("false") boolean trace,
                           @QueryParam("batch") @DefaultValue("false") boolean batch,
                           @QueryParam("bulk") @DefaultValue("false") boolean bulk,
                           @QueryParam("order") @DefaultValue(Order.DEFAULT) String order,
                           @QueryParam("stable") @DefaultValue("true") boolean stable,
                           @HeaderParam("Accept") String accept) {
        final DateTime startTime = new DateTime(year, month, day, 0, 0, 0, 0, DateTimeZone.UTC);
        return this.tagService.getTimeQueryResponse(tag, startTime, location, trace, stable, Unit.DAYS, bulk || batch, accept, uriInfo, epoch, Order.isDescending(order));
    }

    @Path("/{Y}/{M}/{D}/{hour}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getHour(@PathParam("tag") String tag,
                            @PathParam("Y") int year,
                            @PathParam("M") int month,
                            @PathParam("D") int day,
                            @PathParam("hour") int hour,
                            @QueryParam("location") @DefaultValue(Location.DEFAULT) String location,
                            @QueryParam("epoch") @DefaultValue(Epoch.DEFAULT) String epoch,
                            @QueryParam("trace") @DefaultValue("false") boolean trace,
                            @QueryParam("batch") @DefaultValue("false") boolean batch,
                            @QueryParam("bulk") @DefaultValue("false") boolean bulk,
                            @QueryParam("order") @DefaultValue(Order.DEFAULT) String order,
                            @QueryParam("stable") @DefaultValue("true") boolean stable,
                            @HeaderParam("Accept") String accept) {
        final DateTime startTime = new DateTime(year, month, day, hour, 0, 0, 0, DateTimeZone.UTC);
        return this.tagService.getTimeQueryResponse(tag, startTime, location, trace, stable, Unit.HOURS, bulk || batch, accept, uriInfo, epoch, Order.isDescending(order));
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
                              @QueryParam("location") @DefaultValue(Location.DEFAULT) String location,
                              @QueryParam("epoch") @DefaultValue(Epoch.DEFAULT) String epoch,
                              @QueryParam("trace") @DefaultValue("false") boolean trace,
                              @QueryParam("batch") @DefaultValue("false") boolean batch,
                              @QueryParam("bulk") @DefaultValue("false") boolean bulk,
                              @QueryParam("order") @DefaultValue(Order.DEFAULT) String order,
                              @QueryParam("stable") @DefaultValue("true") boolean stable,
                              @HeaderParam("Accept") String accept) {
        final DateTime startTime = new DateTime(year, month, day, hour, minute, 0, 0, DateTimeZone.UTC);
        return this.tagService.getTimeQueryResponse(tag, startTime, location, trace, stable, Unit.MINUTES, bulk || batch, accept, uriInfo, epoch, Order.isDescending(order));
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
                              @QueryParam("location") @DefaultValue(Location.DEFAULT) String location,
                              @QueryParam("epoch") @DefaultValue(Epoch.DEFAULT) String epoch,
                              @QueryParam("trace") @DefaultValue("false") boolean trace,
                              @QueryParam("batch") @DefaultValue("false") boolean batch,
                              @QueryParam("bulk") @DefaultValue("false") boolean bulk,
                              @QueryParam("order") @DefaultValue(Order.DEFAULT) String order,
                              @QueryParam("stable") @DefaultValue("true") boolean stable,
                              @HeaderParam("Accept") String accept) {
        final DateTime startTime = new DateTime(year, month, day, hour, minute, second, 0, DateTimeZone.UTC);
        return this.tagService.getTimeQueryResponse(tag, startTime, location, trace, stable, Unit.SECONDS, bulk || batch, accept, uriInfo, epoch, Order.isDescending(order));
    }

    @Path("/{Y}/{M}/{D}/{h}/{m}/{s}/{ms}/{hash}")
    @GET
    public Response getValue(@PathParam("tag") String tag,
                             @PathParam("Y") int year,
                             @PathParam("M") int month,
                             @PathParam("D") int day,
                             @PathParam("h") int hour,
                             @PathParam("m") int minute,
                             @PathParam("s") int second,
                             @PathParam("ms") int millis,
                             @PathParam("hash") String hash,
                             @HeaderParam("Accept") String accept) {
        final ContentKey key = new ContentKey(year, month, day, hour, minute, second, millis, hash);
        final ItemRequest itemRequest = ItemRequest.builder()
                .tag(tag)
                .key(key)
                .uri(uriInfo.getRequestUri())
                .build();
        final Optional<Content> optionalResult = tagService.getValue(itemRequest);

        if (!optionalResult.isPresent()) {
            log.warn("404 content not found {} {}", tag, key);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        final Content content = optionalResult.get();

        final MediaType actualContentType = ChannelContentResource.getContentType(content);

        if (ChannelContentResource.contentTypeIsNotCompatible(accept, actualContentType)) {
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        final Response.ResponseBuilder builder = Response.ok((StreamingOutput) output -> ByteStreams.copy(content.getStream(), output));

        builder.type(actualContentType)
                .header(CREATION_DATE, TimeUtil.FORMATTER.print(new DateTime(key.getMillis())));

        builder.header("Link", "<" + uriInfo.getRequestUriBuilder().path("previous").build() + ">;rel=\"" + "previous" + "\"");
        builder.header("Link", "<" + uriInfo.getRequestUriBuilder().path("next").build() + ">;rel=\"" + "next" + "\"");
        return builder.build();
    }

    @Path("/{Y}/{M}/{D}/{h}/{m}/{s}/{ms}/{hash}/{direction:[n|p].*}")
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
                                 @QueryParam("location") @DefaultValue(Location.DEFAULT) String location,
                                 @QueryParam("epoch") @DefaultValue(Epoch.DEFAULT) String epoch,
                                 @PathParam("direction") String direction,
                                 @QueryParam("stable") @DefaultValue("true") boolean stable) {
        final ContentKey contentKey = new ContentKey(year, month, day, hour, minute, second, millis, hash);
        return this.tagService.adjacent(tag, contentKey, stable, direction.startsWith("n"), uriInfo, location, epoch);
    }

    @Path("/{Y}/{M}/{D}/{h}/{m}/{s}/{ms}/{hash}/{direction:[n|p].*}/{count}")
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
                                      @QueryParam("location") @DefaultValue(Location.DEFAULT) String location,
                                      @QueryParam("epoch") @DefaultValue(Epoch.DEFAULT) String epoch,
                                      @QueryParam("order") @DefaultValue(Order.DEFAULT) String order,
                                      @HeaderParam("Accept") String accept) {
        final ContentKey key = new ContentKey(year, month, day, hour, minute, second, millis, hash);
        return this.tagService.adjacentCount(tag, count, stable, trace, location, direction.startsWith("n"), key, bulk || batch, accept, uriInfo, epoch, Order.isDescending(order));
    }
}
