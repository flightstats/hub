package com.flightstats.datahub.app.config.metrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Strings;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.spi.dispatch.RequestDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.HttpHeaders;
import java.lang.reflect.AnnotatedElement;

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
            if (!Strings.isNullOrEmpty(contentLength)) {
                meter.mark(Long.parseLong(contentLength));
            }
        } catch (Exception e) {
            logger.warn("throughput is mucked up ", e);
        }

        delegate.dispatch(resource, context);
    }

    private String getMetricName(HttpContext context, PerChannelThroughput annotation) {
        String channelName = ChannelAnnotationUtil.getChannelName(context, annotation.channelNameParameter());
        return "per-channel." + channelName + "." + annotation.operationName();
    }

}
