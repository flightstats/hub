package com.flightstats.hub.util;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

public class ChannelNameUtils {

    public static final String WEBSOCKET_URL_REGEX = "^/channel/(\\w+)/ws$";

    public String extractFromWS(URI requestURI) {
        String path = requestURI.getPath();
        return path.replaceFirst(WEBSOCKET_URL_REGEX, "$1");
    }

    public static String extractFromChannelUrl(URI uri) {
        return extractFromChannelUrl(uri.getPath());
    }

    public static String extractFromChannelUrl(String fullUrl) {
        return StringUtils.substringAfter(fullUrl, "/channel/");
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
