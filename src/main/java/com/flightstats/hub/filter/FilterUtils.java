package com.flightstats.hub.filter;

import org.eclipse.jetty.server.handler.gzip.GzipHandler;

import java.util.Arrays;
import java.util.List;

class FilterUtils {
    static List<String> removedEncodings = Arrays.asList(GzipHandler.GZIP, GzipHandler.DEFLATE);
}
