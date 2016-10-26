package com.flightstats.hub.util;

import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.container.ContainerRequestContext;
import java.net.URI;
import java.net.URISyntaxException;

public class ChannelNameUtils {

    public static String getChannelName(String url) {
        String after = StringUtils.substringAfter(url, "/channel/");
        return StringUtils.removeEnd(after, "/");
    }

    public static String getChannelName(ContainerRequestContext request) {
        String name = request.getUriInfo().getPathParameters().getFirst("channel");
        if (name == null) name = request.getHeaders().getFirst("channelName");
        if (name == null) name = "";
        return name;
    }

    public static boolean isValidChannelUrl(String url) {
        try {
            URI uri = new URI(url);
            return uri.getPath().contains("/channel/");
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
