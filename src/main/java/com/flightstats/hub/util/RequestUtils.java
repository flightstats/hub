package com.flightstats.hub.util;

import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.container.ContainerRequestContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestUtils {

    static final Pattern hostNamePattern = Pattern.compile("(^.*\\:\\/\\/[^\\/]*)(\\/.*)?");

    private static String getTopicParamFromUrl(String url, String topic) {
        String after = StringUtils.substringAfter(url, "/" + topic + "/");
        return StringUtils.removeEnd(after, "/");
    }

    public static String getChannelName(String url) {
        return getTopicParamFromUrl(url, "channel");
    }

    public static String getTag(String url) {
        return getTopicParamFromUrl(url, "tag");
    }

    public static String getHost(String url) {
        String result = "";
        try {
            Matcher m = hostNamePattern.matcher(url);
            m.find();
            result = m.group(1);
        } catch (Exception e) {
        }
        return result;
    }

    public static String getChannelName(ContainerRequestContext request) {
        String name = request.getUriInfo().getPathParameters().getFirst("channel");
        if (name == null) name = request.getHeaders().getFirst("channelName");
        if (name == null) name = "";
        return name;
    }

    public static String getTag(ContainerRequestContext request) {
        String tag = request.getUriInfo().getPathParameters().getFirst("tag");
        if (tag == null) tag = "";
        return tag;
    }

    private static boolean isValidTopicUrl(String url, String topic) {
        try {
            URI uri = new URI(url);
            return uri.getPath().contains("/" + topic + "/");
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public static boolean isValidChannelUrl(String url) {
        return isValidTopicUrl(url, "channel");
    }

    public static boolean isValidTagUrl(String url) {
        return isValidTopicUrl(url, "tag");
    }
}
