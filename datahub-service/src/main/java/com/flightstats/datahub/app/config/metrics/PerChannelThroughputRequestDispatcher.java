package com.flightstats.datahub.app.config.metrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.server.impl.application.WebApplicationContext;
import com.sun.jersey.spi.dispatch.RequestDispatcher;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import java.lang.reflect.AnnotatedElement;
import java.util.List;

class PerChannelThroughputRequestDispatcher implements RequestDispatcher {
    private final static Logger logger = LoggerFactory.getLogger(PerChannelThroughputRequestDispatcher.class);

    private final MetricRegistry registry;
    private final AnnotatedElement annotatedElement;
    private final RequestDispatcher delegate;

    PerChannelThroughputRequestDispatcher(MetricRegistry metricRegistry, AnnotatedElement annotatedElement, RequestDispatcher delegate) {
        this.registry = metricRegistry;
        this.annotatedElement = annotatedElement;
        this.delegate = delegate;
    }

    @Override
    public void dispatch(final Object resource, final HttpContext context) {
        PerChannelThroughput throughputAnnotation = annotatedElement.getAnnotation(PerChannelThroughput.class);
        if (throughputAnnotation == null) {
            delegate.dispatch(resource, context);
            return;
        }
        String metricName = getMetricName(context, throughputAnnotation);
        Meter meter = registry.meter(metricName);
        try {
            String contentLength = context.getRequest().getHeaderValue(HttpHeaders.CONTENT_LENGTH);
            if (StringUtils.isNotBlank(contentLength)) {
                meter.mark(Long.parseLong(contentLength));
            }
        } catch (Exception e) {
            logger.warn("throughput is mucked up ", e);
        }

        delegate.dispatch(resource, context);
    }

    private String getMetricName(HttpContext context, PerChannelThroughput throughputAnnotation) {
        String channelName = getChannelName(context, throughputAnnotation);
        return "per-channel." + channelName + "." + throughputAnnotation.operationName();
    }

    private String getChannelName(HttpContext context, PerChannelThroughput throughputAnnotation) {
        MultivaluedMap<String, String> pathParameters = ((WebApplicationContext) context).getPathParameters(true);
        String channelNamePathParam = throughputAnnotation.channelNamePathParameter();
        List<String> channelNames = pathParameters.get(channelNamePathParam);
        if (channelNames == null || channelNames.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unable to determine channel name for metrics.  There is no parameter named " + channelNamePathParam);
        }
        return channelNames.get(0);
    }
}
