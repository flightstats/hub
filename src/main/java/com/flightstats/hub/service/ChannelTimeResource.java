package com.flightstats.hub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static com.flightstats.hub.util.TimeUtil.*;
import static javax.ws.rs.core.Response.Status.SEE_OTHER;

/**
 * This resource redirects to the new time interface
 */
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
        DateTime stable = stable();
        addNode(links, "second", "/{year}/{month}/{day}/{hour}/{minute}/{second}", seconds(stable));
        addNode(links, "minute", "/{year}/{month}/{day}/{hour}/{minute}", minutes(stable));
        addNode(links, "hour", "/{year}/{month}/{day}/{hour}", hours(stable));
        addNode(links, "day", "/{year}/{month}/{day}", days(stable));
        return Response.ok(root).build();
    }

    private void addNode(ObjectNode links, String name, String template, String time) {
        ObjectNode second = links.putObject(name);
        String requestUri = StringUtils.removeEnd(uriInfo.getRequestUri().toString(), "time");
        second.put("href", requestUri + time);
        second.put("template", uriInfo.getRequestUri() + template);
        second.put("redirect", uriInfo.getRequestUri() + "/" + name);
    }

    private ObjectNode addSelfLink(ObjectNode root) {
        ObjectNode links = root.putObject("_links");
        ObjectNode self = links.putObject("self");
        self.put("href", uriInfo.getRequestUri().toString());
        return links;
    }

    @Path("/second")
    @GET
    public Response getSecond() {
        return getResponse(seconds(stable()), "time/second");
    }

    @Path("/minute")
    @GET
    public Response getMinute() {
        return getResponse(minutes(stable()), "time/minute");
    }

    @Path("/hour")
    @GET
    public Response getHour() {
        return getResponse(hours(stable()), "time/hour");
    }

    @Path("/day")
    @GET
    public Response getDay() {
        return getResponse(days(stable()), "time/day");
    }

    private Response getResponse(String timePath, String endString) {
        Response.ResponseBuilder builder = Response.status(SEE_OTHER);
        String channelUri = uriInfo.getRequestUri().toString();
        channelUri = StringUtils.removeEnd(channelUri, endString);
        URI uri = URI.create(channelUri + timePath);
        builder.location(uri);
        return builder.build();
    }
}
