package com.flightstats.datahub.app.config.metrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.uri.UriTemplate;
import com.sun.jersey.server.impl.application.WebApplicationContext;
import com.sun.jersey.server.impl.application.WebApplicationImpl;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.dispatch.RequestDispatcher;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;
import java.util.regex.MatchResult;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class PerChannelThroughputRequestDispatcherTest {

	@Test
	public void testMethodNotAnnotated() throws Exception {
		Object resource = new Object();

		AnnotatedElement annotatedElement = mock(AnnotatedElement.class);
		MetricRegistry registry = mock(MetricRegistry.class);
		RequestDispatcher delegate = mock(RequestDispatcher.class);
		HttpContext context = mock(HttpContext.class);

		when(annotatedElement.getAnnotation(any(Class.class))).thenReturn(null);

		PerChannelThroughputRequestDispatcher testClass = new PerChannelThroughputRequestDispatcher(registry, annotatedElement, delegate);

		testClass.dispatch(resource, context);

		verifyNoMoreInteractions(registry);
		verify(delegate).dispatch(resource, context);
	}

    @Test
    public void testHappyPath() throws Exception {
        Object resource = new Object();
        String name = "per-channel.theSpoon.invert";
        String channelName = "theSpoon";
        List<String> paramNames = Arrays.asList("channelName");

        ContainerRequest requestContext = mock(ContainerRequest.class);
        when(requestContext.getHeaderValue(HttpHeaders.CONTENT_LENGTH)).thenReturn("12345");
        WebApplicationContext context = new WebApplicationContext(new WebApplicationImpl(), requestContext, null);

        AnnotatedElement annotatedElement = mock(AnnotatedElement.class);
        MetricRegistry registry = mock(MetricRegistry.class);
        RequestDispatcher delegate = mock(RequestDispatcher.class);
        UriTemplate uriTemplate = mock(UriTemplate.class);
        MatchResult matchResult = mock(MatchResult.class);
        PerChannelThroughput annotation = mock(PerChannelThroughput.class);
        Meter meter = mock(Meter.class);

        when(annotatedElement.getAnnotation(PerChannelThroughput.class)).thenReturn(annotation);
        when(annotation.channelNameParameter()).thenReturn("channelName");
        when(annotation.operationName()).thenReturn("invert");
        when(registry.meter(name)).thenReturn(meter);
        
        when(matchResult.group(anyInt())).thenReturn(channelName);

        context.setMatchResult(matchResult);
        context.pushMatch(uriTemplate, paramNames);

        PerChannelThroughputRequestDispatcher testClass = new PerChannelThroughputRequestDispatcher(registry, annotatedElement, delegate);

        testClass.dispatch(resource, context);

        verify(delegate).dispatch(resource, context);
        verify(meter).mark(12345);
    }

    @Test
    public void testNullEntity() throws Exception {
        Object resource = new Object();
        String name = "per-channel.theSpoon.invert";
        String channelName = "theSpoon";
        List<String> paramNames = Arrays.asList("channelName");

        ContainerRequest requestContext = mock(ContainerRequest.class);
        when(requestContext.getEntity(byte[].class)).thenReturn(null);
        WebApplicationContext context = new WebApplicationContext(new WebApplicationImpl(), requestContext, null);

        AnnotatedElement annotatedElement = mock(AnnotatedElement.class);
        MetricRegistry registry = mock(MetricRegistry.class);
        RequestDispatcher delegate = mock(RequestDispatcher.class);
        UriTemplate uriTemplate = mock(UriTemplate.class);
        MatchResult matchResult = mock(MatchResult.class);
        PerChannelThroughput annotation = mock(PerChannelThroughput.class);
        Meter meter = mock(Meter.class);

        when(annotatedElement.getAnnotation(PerChannelThroughput.class)).thenReturn(annotation);
        when(annotation.channelNameParameter()).thenReturn("channelName");
        when(annotation.operationName()).thenReturn("invert");
        when(registry.meter(name)).thenReturn(meter);

        when(matchResult.group(anyInt())).thenReturn(channelName);

        context.setMatchResult(matchResult);
        context.pushMatch(uriTemplate, paramNames);

        PerChannelThroughputRequestDispatcher testClass = new PerChannelThroughputRequestDispatcher(registry, annotatedElement, delegate);

        testClass.dispatch(resource, context);

        verify(delegate).dispatch(resource, context);
    }

    @Test(expected = IllegalArgumentException.class)
	public void testCantFindChannelName() throws Exception {
		Object resource = new Object();
		String channelName = "theSpoon";
		List<String> paramNames = Arrays.asList("notTheRightParam", "alsoTheWrongParamter");
		WebApplicationContext context = new WebApplicationContext(new WebApplicationImpl(), null, null);

		AnnotatedElement annotatedElement = mock(AnnotatedElement.class);
		MetricRegistry registry = mock(MetricRegistry.class);
		RequestDispatcher delegate = mock(RequestDispatcher.class);
		UriTemplate uriTemplate = mock(UriTemplate.class);
		MatchResult matchResult = mock(MatchResult.class);
		PerChannelThroughput annotation = mock(PerChannelThroughput.class);

		when(annotatedElement.getAnnotation(PerChannelThroughput.class)).thenReturn(annotation);
		when(annotation.channelNameParameter()).thenReturn("channelName");
		when(annotation.operationName()).thenReturn("invert");
		when(matchResult.group(anyInt())).thenReturn(channelName);

		context.setMatchResult(matchResult);
		context.pushMatch(uriTemplate, paramNames);

		PerChannelThroughputRequestDispatcher testClass = new PerChannelThroughputRequestDispatcher(registry, annotatedElement, delegate);

		testClass.dispatch(resource, context);
	}

    @Test
    public void testHeader() throws Exception {
        Object resource = new Object();
        String name = "per-channel.theSpoon.invert";
        String channelName = "theSpoon";

        ContainerRequest request = mock(ContainerRequest.class);
        when(request.getHeaderValue(HttpHeaders.CONTENT_LENGTH)).thenReturn("12345");
        when(request.getHeaderValue("channelName")).thenReturn(channelName);
        WebApplicationContext context = new WebApplicationContext(new WebApplicationImpl(), request, null);

        AnnotatedElement annotatedElement = mock(AnnotatedElement.class);
        MetricRegistry registry = mock(MetricRegistry.class);
        RequestDispatcher delegate = mock(RequestDispatcher.class);
        PerChannelThroughput annotation = mock(PerChannelThroughput.class);
        Meter meter = mock(Meter.class);

        when(annotatedElement.getAnnotation(PerChannelThroughput.class)).thenReturn(annotation);
        when(annotation.channelNameParameter()).thenReturn("channelName");
        when(annotation.operationName()).thenReturn("invert");
        when(registry.meter(name)).thenReturn(meter);

        PerChannelThroughputRequestDispatcher testClass = new PerChannelThroughputRequestDispatcher(registry, annotatedElement, delegate);

        testClass.dispatch(resource, context);

        verify(delegate).dispatch(resource, context);
        verify(meter).mark(12345);
    }
}
