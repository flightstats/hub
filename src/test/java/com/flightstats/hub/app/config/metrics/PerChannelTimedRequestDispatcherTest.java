package com.flightstats.hub.app.config.metrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.uri.UriTemplate;
import com.sun.jersey.server.impl.application.WebApplicationContext;
import com.sun.jersey.server.impl.application.WebApplicationImpl;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.dispatch.RequestDispatcher;
import org.junit.Test;
import org.mockito.internal.verification.AtMost;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;
import java.util.regex.MatchResult;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class PerChannelTimedRequestDispatcherTest {

	@Test
	public void testMethodNotAnnotated() throws Exception {
		Object resource = new Object();

		AnnotatedElement annotatedElement = mock(AnnotatedElement.class);
		MetricRegistry registry = mock(MetricRegistry.class);
		RequestDispatcher delegate = mock(RequestDispatcher.class);
		HttpContext context = mock(HttpContext.class);

		when(annotatedElement.getAnnotation(any(Class.class))).thenReturn(null);

		PerChannelTimedRequestDispatcher testClass = new PerChannelTimedRequestDispatcher(registry, annotatedElement, delegate, null);

		testClass.dispatch(resource, context);

		verifyNoMoreInteractions(registry);
		verify(delegate).dispatch(resource, context);
	}

    @Test
    public void testHappyPath() throws Exception {
        Object resource = new Object();
        String timerName = "channel.theSpoon.invert";
        String channelName = "theSpoon";
        List<String> paramNames = Arrays.asList("channelName");
        WebApplicationContext context = new WebApplicationContext(new WebApplicationImpl(), null, null);

        AnnotatedElement annotatedElement = mock(AnnotatedElement.class);
        MetricRegistry registry = mock(MetricRegistry.class);
        RequestDispatcher delegate = mock(RequestDispatcher.class);
        UriTemplate uriTemplate = mock(UriTemplate.class);
        MatchResult matchResult = mock(MatchResult.class);
        PerChannelTimed annotation = mock(PerChannelTimed.class);
        Timer.Context timerContext = mock(Timer.Context.class);
        Timer timer = mock(Timer.class);

        when(annotatedElement.getAnnotation(PerChannelTimed.class)).thenReturn(annotation);
        when(annotation.channelNameParameter()).thenReturn("channelName");
        when(annotation.operationName()).thenReturn("invert");
        when(registry.timer(timerName)).thenReturn(timer);
        when(timer.time()).thenReturn(timerContext);
        when(matchResult.group(anyInt())).thenReturn(channelName);

        context.setMatchResult(matchResult);
        context.pushMatch(uriTemplate, paramNames);

        PerChannelTimedRequestDispatcher testClass = new PerChannelTimedRequestDispatcher(registry, annotatedElement, delegate, mock(HostedGraphiteSender.class));

        testClass.dispatch(resource, context);

        verify(timer).time();
        verify(delegate).dispatch(resource, context);
        verify(timerContext).close();
    }

    @Test
    public void testNoTestChannels() throws Exception {
        Object resource = new Object();
        String timerName = "channel.theSpoon.invert";
        String channelName = "testBlah";
        List<String> paramNames = Arrays.asList("channelName");
        WebApplicationContext context = new WebApplicationContext(new WebApplicationImpl(), null, null);

        AnnotatedElement annotatedElement = mock(AnnotatedElement.class);
        MetricRegistry registry = mock(MetricRegistry.class);
        RequestDispatcher delegate = mock(RequestDispatcher.class);
        UriTemplate uriTemplate = mock(UriTemplate.class);
        MatchResult matchResult = mock(MatchResult.class);
        PerChannelTimed annotation = mock(PerChannelTimed.class);
        Timer.Context timerContext = mock(Timer.Context.class);
        Timer timer = mock(Timer.class);

        when(annotatedElement.getAnnotation(PerChannelTimed.class)).thenReturn(annotation);
        when(annotation.channelNameParameter()).thenReturn("channelName");
        when(annotation.operationName()).thenReturn("invert");
        when(registry.timer(timerName)).thenReturn(timer);
        when(timer.time()).thenReturn(timerContext);
        when(matchResult.group(anyInt())).thenReturn(channelName);

        context.setMatchResult(matchResult);
        context.pushMatch(uriTemplate, paramNames);

        PerChannelTimedRequestDispatcher testClass = new PerChannelTimedRequestDispatcher(registry, annotatedElement, delegate, mock(HostedGraphiteSender.class));

        testClass.dispatch(resource, context);

        verify(timer, new AtMost(0)).time();
        verify(delegate).dispatch(resource, context);
    }

    @Test
    public void testExceptionPath() throws Exception {
        //GIVEN
        Object resource = new Object();
        String timerName = "channel.theSpoon.invert";
        String channelName = "theSpoon";
        List<String> paramNames = Arrays.asList("channelName");
        WebApplicationContext context = new WebApplicationContext(new WebApplicationImpl(), null, null);

        AnnotatedElement annotatedElement = mock(AnnotatedElement.class);
        MetricRegistry registry = mock(MetricRegistry.class);
        RequestDispatcher delegate = mock(RequestDispatcher.class);
        UriTemplate uriTemplate = mock(UriTemplate.class);
        MatchResult matchResult = mock(MatchResult.class);
        PerChannelTimed annotation = mock(PerChannelTimed.class);
        Meter exceptionMeter = mock(Meter.class);
        Timer.Context timerContext = mock(Timer.Context.class);
        Timer timer = mock(Timer.class);

        when(annotatedElement.getAnnotation(PerChannelTimed.class)).thenReturn(annotation);
        when(annotation.channelNameParameter()).thenReturn("channelName");
        when(annotation.operationName()).thenReturn("invert");
        when(registry.timer(timerName)).thenReturn(timer);
        when(registry.meter(timerName + ".exceptions")).thenReturn(exceptionMeter);
        when(timer.time()).thenReturn(timerContext);
        when(matchResult.group(anyInt())).thenReturn(channelName);
        doThrow(new RuntimeException()).when(delegate).dispatch(anyObject(), any(HttpContext.class));

        context.setMatchResult(matchResult);
        context.pushMatch(uriTemplate, paramNames);

        PerChannelTimedRequestDispatcher testClass = new PerChannelTimedRequestDispatcher(registry, annotatedElement, delegate, null);

        try {
            testClass.dispatch(resource, context);
            fail("dispatch() should have thrown an exception");
        } catch ( RuntimeException ignored ) {
        }

        verify(timer).time();
        verify(exceptionMeter).mark();
        verify(delegate).dispatch(resource, context);
        verify(timerContext).close();
    }

    @Test(expected = IllegalArgumentException.class)
	public void testCantFindChannelName() throws Exception {
		Object resource = new Object();
		String timerName = "channel.theSpoon.invert";
		String channelName = "theSpoon";
		List<String> paramNames = Arrays.asList("notTheRightParam", "alsoTheWrongParamter");
		WebApplicationContext context = new WebApplicationContext(new WebApplicationImpl(), null, null);

		AnnotatedElement annotatedElement = mock(AnnotatedElement.class);
		MetricRegistry registry = mock(MetricRegistry.class);
		RequestDispatcher delegate = mock(RequestDispatcher.class);
		UriTemplate uriTemplate = mock(UriTemplate.class);
		MatchResult matchResult = mock(MatchResult.class);
		PerChannelTimed annotation = mock(PerChannelTimed.class);
		Timer.Context timerContext = mock(Timer.Context.class);
		Timer timer = mock(Timer.class);

		when(annotatedElement.getAnnotation(PerChannelTimed.class)).thenReturn(annotation);
		when(annotation.channelNameParameter()).thenReturn("channelName");
		when(annotation.operationName()).thenReturn("invert");
		when(registry.timer(timerName)).thenReturn(timer);
		when(timer.time()).thenReturn(timerContext);
		when(matchResult.group(anyInt())).thenReturn(channelName);

		context.setMatchResult(matchResult);
		context.pushMatch(uriTemplate, paramNames);

		PerChannelTimedRequestDispatcher testClass = new PerChannelTimedRequestDispatcher(registry, annotatedElement, delegate, null);

		testClass.dispatch(resource, context);
	}

    @Test
    public void testHeader() throws Exception {
        Object resource = new Object();
        String timerName = "channel.theSpoon.invert";
        String channelName = "theSpoon";
        ContainerRequest request = mock(ContainerRequest.class);
        when(request.getHeaderValue("channelName")).thenReturn(channelName);
        WebApplicationContext context = new WebApplicationContext(new WebApplicationImpl(), request, null);

        AnnotatedElement annotatedElement = mock(AnnotatedElement.class);
        MetricRegistry registry = mock(MetricRegistry.class);
        RequestDispatcher delegate = mock(RequestDispatcher.class);
        PerChannelTimed annotation = mock(PerChannelTimed.class);
        Timer.Context timerContext = mock(Timer.Context.class);
        Timer timer = mock(Timer.class);

        when(annotatedElement.getAnnotation(PerChannelTimed.class)).thenReturn(annotation);
        when(annotation.channelNameParameter()).thenReturn("channelName");
        when(annotation.operationName()).thenReturn("invert");
        when(registry.timer(timerName)).thenReturn(timer);
        when(timer.time()).thenReturn(timerContext);

        PerChannelTimedRequestDispatcher dispatcher = new PerChannelTimedRequestDispatcher(registry, annotatedElement, delegate, mock(HostedGraphiteSender.class));

        dispatcher.dispatch(resource, context);

        verify(timer).time();
        verify(delegate).dispatch(resource, context);
        verify(timerContext).close();
    }
}
