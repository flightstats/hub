package com.flightstats.hub.app.config.metrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.spi.dispatch.RequestDispatcher;

import java.lang.reflect.AnnotatedElement;

class PerChannelTimedRequestDispatcher implements RequestDispatcher {
    private final MetricRegistry registry;
    private final AnnotatedElement annotatedElement;
    private final RequestDispatcher delegate;
    private final HostedGraphiteSender sender;

    PerChannelTimedRequestDispatcher(MetricRegistry metricRegistry, AnnotatedElement annotatedElement,
                                     RequestDispatcher delegate, HostedGraphiteSender sender) {
        this.registry = metricRegistry;
        this.annotatedElement = annotatedElement;
        this.delegate = delegate;
        this.sender = sender;
    }

    @Override
    public void dispatch(final Object resource, final HttpContext context) {
        PerChannelTimed timedAnnotation = annotatedElement.getAnnotation(PerChannelTimed.class);
        if (timedAnnotation == null) {
            delegate.dispatch(resource, context);
            return;
        }

        String metricName = getMetricName(context, timedAnnotation);
        long start = System.currentTimeMillis();
        final Meter exceptionMeter = registry.meter(metricName + ".exceptions");
        try (Timer.Context ignored = buildTimerContext(metricName)) {
            delegate.dispatch(resource, context);
        } catch (Exception e) {
            exceptionMeter.mark();
            throw e;
        } finally {
            sender.send(metricName, System.currentTimeMillis() - start);
        }
    }

    private Timer.Context buildTimerContext(String metricName) {
        Timer timer = registry.timer(metricName);
        return timer.time();
    }

    private String getMetricName(HttpContext context, PerChannelTimed timedAnnotation) {
        String channelName = ChannelAnnotationUtil.getChannelName(context, timedAnnotation.channelNameParameter());
        return "per-channel." + channelName + "." + timedAnnotation.operationName();
    }

}
