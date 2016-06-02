package com.flightstats.hub.filter;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.util.ChannelNameUtils;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;
import org.glassfish.jersey.uri.UriTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter class to handle intercepting requests and responses from the Hub and pipe statistics to
 * DogStatsD (DataDog) agent running on the server.
 */
@Provider
public class DataDogRequestFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger logger = LoggerFactory.getLogger(DataDogRequestFilter.class);

    public static final String HUB_DATADOG_METRICS_FLAG = "data_dog.enable";

    private final static StatsDClient statsd = new NonBlockingStatsDClient(null, "localhost", 8125, new String[]{"tag:value"});
    private static final ThreadLocal<Long> threadStartTime = new ThreadLocal();
    private final boolean isDataDogActive;

    public DataDogRequestFilter() {
        isDataDogActive = HubProperties.getProperty(HUB_DATADOG_METRICS_FLAG, false);
    }


    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        if (isDataDogActive) {
            List<String> tags = new ArrayList<>();
            String channelName = ChannelNameUtils.parseChannelName(request.getUriInfo().getRequestUri().getPath());
            if(channelName != null) addTag(tags, "channel", channelName);
            addTag(tags, "method", request.getMethod());
            addTag(tags, "endpoint", getRequestTemplate(request));
            long time = System.currentTimeMillis() - threadStartTime.get();
            statsd.time("hub.request", time, tags.toArray(new String[tags.size()]));

            // report any errors
            int returnCode = response.getStatus();
            if (returnCode > 400 && returnCode != 404) {
                statsd.incrementCounter("hub.errors", new String[]{"errorCode:" + returnCode});
            }
        }
    }

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        if (isDataDogActive) {
            threadStartTime.set(System.currentTimeMillis());
        }
    }

    private List<String> addTag(List<String> tags, String tagName, String value){
        if(tags == null){ tags = new ArrayList<String>(); }
        tags.add(tagName + ":" + value);
        return tags;
    }

    private String getRequestTemplate(ContainerRequestContext request) {
        UriRoutingContext uriInfo = (UriRoutingContext) request.getUriInfo();
        ArrayList<UriTemplate> templateList = new ArrayList<>(uriInfo.getMatchedTemplates());
        Collections.reverse(templateList);
        return templateList
                .stream()
                .map(UriTemplate::getTemplate)
                .collect(Collectors.joining(""));
    }
}
