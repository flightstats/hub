package com.flightstats.hub.app.config.metrics;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.server.impl.application.WebApplicationContext;

import javax.ws.rs.core.MultivaluedMap;
import java.util.List;

public class ChannelAnnotationUtil {

    public static String getChannelName(HttpContext context, String channelNameParam) {
        try {
            MultivaluedMap<String, String> pathParameters = ((WebApplicationContext) context).getPathParameters(true);
            List<String> channelNames = pathParameters.get(channelNameParam);
            if (channelNames != null && !channelNames.isEmpty()) {
                return channelNames.get(0);
            }
        } catch (Exception e) {
            //ignoring
        }
        if (context.getRequest() != null) {
            String headerValue = context.getRequest().getHeaderValue(channelNameParam);
            if (headerValue != null) {
                return headerValue;
            }
        }
        throw new IllegalArgumentException("Unable to determine channel name for metrics.  There is no parameter named " + channelNameParam);
    }
}
