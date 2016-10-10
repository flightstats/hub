package com.flightstats.hub.util;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChannelNameUtils {

    private static final String WEBSOCKET_URL_REGEX = "^/channel/(\\w+)/ws$";
    private static final Pattern DEFAULT_NAME_PATTERN = Pattern.compile("/channel/([^\\/]*)/?.*$");
    private static final Pattern S3BATCH_NAME_PATTERN = Pattern.compile("/internal/s3Batch/(\\w+)");

    public String extractFromWS(URI requestURI) {
        String path = requestURI.getPath();
        return path.replaceFirst(WEBSOCKET_URL_REGEX, "$1");
    }

    public static String extractFromChannelUrl(URI uri) {
        return extractFromChannelUrl(uri.getPath());
    }

    public static String extractFromChannelUrl(String fullUrl) {
        String after = StringUtils.substringAfter(fullUrl, "/channel/");
        return StringUtils.removeEnd(after, "/");
    }

    public static String parseChannelName(String path) {
        String name = checkPattern(path, DEFAULT_NAME_PATTERN);
        if (name.isEmpty()) {
            name = checkPattern(path, S3BATCH_NAME_PATTERN);
        }

        return name.isEmpty() ? null : name;
    }

    private static String checkPattern(String path, Pattern pattern) {
        Matcher m = pattern.matcher(path);
        return m.find() ? m.group(1) : "";
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
