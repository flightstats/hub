package com.flightstats.hub.app.config.metrics;

import com.flightstats.hub.metrics.HostedGraphiteSender;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.spi.dispatch.RequestDispatcher;

import java.lang.reflect.AnnotatedElement;

class PerChannelTimedRequestDispatcher implements RequestDispatcher {
    private final AnnotatedElement annotatedElement;
    private final RequestDispatcher delegate;
    private final HostedGraphiteSender sender;

    PerChannelTimedRequestDispatcher(AnnotatedElement annotatedElement,
                                     RequestDispatcher delegate, HostedGraphiteSender sender) {
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
        String channelName = ChannelAnnotationUtil.getChannelName(context, timedAnnotation.channelNameParameter());
        if (channelName.startsWith("test")) {
            delegate.dispatch(resource, context);
            return;
        }
        long start = System.currentTimeMillis();

        try {
            delegate.dispatch(resource, context);
        } finally {
            sender.send("channel." + channelName + "." + timedAnnotation.operationName(),
                    System.currentTimeMillis() - start);
        }
    }

}
