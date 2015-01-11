package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.util.TimeUtil;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static com.flightstats.hub.util.TimeUtil.*;
import static javax.ws.rs.core.Response.Status.SEE_OTHER;

@Path("/channel/{channelName: .*}/time")
public class ChannelTimeResource {

    private final static Logger logger = LoggerFactory.getLogger(ChannelTimeResource.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private final UriInfo uriInfo;

    @Inject
    public ChannelTimeResource(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDefault() {
        ObjectNode root = mapper.createObjectNode();
        ObjectNode links = addSelfLink(root);
        addNode(links, "second", "/{year}/{month}/{day}/{hour}/{minute}/{second}", Unit.SECONDS);
        addNode(links, "minute", "/{year}/{month}/{day}/{hour}/{minute}", Unit.MINUTES);
        addNode(links, "hour", "/{year}/{month}/{day}/{hour}", Unit.HOURS);
        addNode(links, "day", "/{year}/{month}/{day}", Unit.DAYS);
        DateTime now = TimeUtil.now();
        DateTime stable = TimeUtil.stable();
        addTime(root, now, "now");
        addTime(root, stable, "stable");
        return Response.ok(root).build();
    }

    private void addTime(ObjectNode root, DateTime time, String name) {
        ObjectNode nowNode = root.putObject(name);
        nowNode.put("iso8601", ISODateTimeFormat.dateTime().print(time));
        nowNode.put("millis", time.getMillis());
    }

    private void addNode(ObjectNode links, String name, String template, Unit unit) {
        ObjectNode node = links.putObject(name);
        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
        String stable = "";
        DateTime dateTime = stable();
        if (queryParameters.containsKey("stable")) {
            String value = queryParameters.getFirst("stable");
            stable = "?stable=" + value;
            if (!Boolean.parseBoolean(value)) {
                dateTime = now();
            }
        }
        String requestUri = StringUtils.removeEnd(uriInfo.getAbsolutePath().toString(), "time");
        node.put("href", requestUri + unit.format(dateTime) + stable);
        node.put("template", uriInfo.getAbsolutePath() + template + "{?stable}");
        node.put("redirect", uriInfo.getAbsolutePath() + "/" + name + stable);
    }

    private ObjectNode addSelfLink(ObjectNode root) {
        ObjectNode links = root.putObject("_links");
        ObjectNode self = links.putObject("self");
        self.put("href", uriInfo.getRequestUri().toString());
        return links;
    }

    @Path("/second")
    @GET
    public Response getSecond(@QueryParam("stable") @DefaultValue("true") boolean stable) {
        return getResponse(seconds(getDateTime(stable)) + "?stable=" + stable, "time/second");
    }

    @Path("/minute")
    @GET
    public Response getMinute(@QueryParam("stable") @DefaultValue("true") boolean stable,
                              @QueryParam("trace") @DefaultValue("false") boolean trace) {
        return getResponse(minutes(getDateTime(stable)) + "?stable=" + stable + "&trace=" + trace, "time/minute");
    }

    @Path("/hour")
    @GET
    public Response getHour(@QueryParam("stable") @DefaultValue("true") boolean stable) {
        return getResponse(hours(getDateTime(stable)) + "?stable=" + stable, "time/hour");
    }

    @Path("/day")
    @GET
    public Response getDay(@QueryParam("stable") @DefaultValue("true") boolean stable) {
        return getResponse(days(getDateTime(stable)) + "?stable=" + stable, "time/day");
    }

    private DateTime getDateTime(boolean stable) {
        DateTime dateTime = TimeUtil.stable();
        if (!stable) {
            dateTime = TimeUtil.now();
        }
        return dateTime;
    }

    private Response getResponse(String timePath, String endString) {
        Response.ResponseBuilder builder = Response.status(SEE_OTHER);
        String channelUri = uriInfo.getAbsolutePath().toString();
        channelUri = StringUtils.removeEnd(channelUri, endString);
        URI uri = URI.create(channelUri + timePath);
        builder.location(uri);
        return builder.build();
    }
}
