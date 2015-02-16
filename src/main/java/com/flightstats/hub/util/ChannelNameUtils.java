package com.flightstats.hub.util;

import java.net.URI;
import java.net.URISyntaxException;

public class ChannelNameUtils {

    public static final String WEBSOCKET_URL_REGEX = "^/channel/(\\w+)/ws$";

    public String extractFromWS(URI requestURI) {
        String path = requestURI.getPath();
        return path.replaceFirst(WEBSOCKET_URL_REGEX, "$1");
    }

    public static String extractFromChannelUrl(URI uri) {
        String path = uri.getPath();
        return path.replaceAll("/channel/(\\w+)(/?)", "$1");
    }

    public static String extractFromChannelUrl(String fullUrl) {
        return extractFromChannelUrl(URI.create(fullUrl));
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
