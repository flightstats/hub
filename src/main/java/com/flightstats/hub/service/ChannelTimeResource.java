package com.flightstats.hub.service;

import com.flightstats.hub.util.TimeUtil;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static javax.ws.rs.core.Response.Status.SEE_OTHER;

/**
 * This resource redirects to the new time interface
 */
@Path("/channel/{channelName: .*}/time")
public class ChannelTimeResource {

    private final static Logger logger = LoggerFactory.getLogger(ChannelTimeResource.class);
    private final UriInfo uriInfo;

    @Inject
    public ChannelTimeResource(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    @GET
    public Response getDefault() {
        return getResponse(TimeUtil.secondsNow(), "time");
    }

    @Path("/second")
    @GET
    public Response getSecond() {
        return getResponse(TimeUtil.secondsNow(), "time/second");
    }

    @Path("/minute")
    @GET
    public Response getMinute() {
        return getResponse(TimeUtil.minutesNow(), "time/minute");
    }

    @Path("/hour")
    @GET
    public Response getHour() {
        return getResponse(TimeUtil.hoursNow(), "time/hour");
    }

    @Path("/day")
    @GET
    public Response getDay() {
        return getResponse(TimeUtil.daysNow(), "time/day");
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
