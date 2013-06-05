package com.flightstats.datahub.app.config.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.uri.UriTemplate;
import com.sun.jersey.server.impl.application.WebApplicationContext;
import com.sun.jersey.server.impl.application.WebApplicationImpl;
import com.sun.jersey.spi.dispatch.RequestDispatcher;
import org.junit.Test;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;
import java.util.regex.MatchResult;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class PerChannelTimedRequestDispatcherTest {

	@Test
	public void testMethodNotAnnotated() throws Exception {
		//GIVEN
		Object resource = new Object();

		AnnotatedElement annotatedElement = mock(AnnotatedElement.class);
		MetricRegistry registry = mock(MetricRegistry.class);
		RequestDispatcher delegate = mock(RequestDispatcher.class);
		HttpContext context = mock(HttpContext.class);

		when(annotatedElement.getAnnotation(any(Class.class))).thenReturn(null);

		PerChannelTimedRequestDispatcher testClass = new PerChannelTimedRequestDispatcher(registry, annotatedElement, delegate);

		//WHEN
		testClass.dispatch(resource, context);

		//THEN
		verifyNoMoreInteractions(registry);
		verify(delegate).dispatch(resource, context);
	}

	@Test
	public void testHappyPath() throws Exception {
		//GIVEN
		Object resource = new Object();
		String timerName = "per-channel.theSpoon.invert";
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
		when(annotation.channelNamePathParameter()).thenReturn("channelName");
		when(annotation.operationName()).thenReturn("invert");
		when(registry.timer(timerName)).thenReturn(timer);
		when(timer.time()).thenReturn(timerContext);
		when(matchResult.group(anyInt())).thenReturn(channelName);

		context.setMatchResult(matchResult);
		context.pushMatch(uriTemplate, paramNames);

		PerChannelTimedRequestDispatcher testClass = new PerChannelTimedRequestDispatcher(registry, annotatedElement, delegate);

		//WHEN
		testClass.dispatch(resource, context);

		//THEN
		verify(timer).time();
		verify(delegate).dispatch(resource, context);
		verify(timerContext).stop();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCantFindChannelName() throws Exception {
		//GIVEN
		Object resource = new Object();
		String timerName = "per-channel.theSpoon.invert";
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
		when(annotation.channelNamePathParameter()).thenReturn("channelName");
		when(annotation.operationName()).thenReturn("invert");
		when(registry.timer(timerName)).thenReturn(timer);
		when(timer.time()).thenReturn(timerContext);
		when(matchResult.group(anyInt())).thenReturn(channelName);

		context.setMatchResult(matchResult);
		context.pushMatch(uriTemplate, paramNames);

		PerChannelTimedRequestDispatcher testClass = new PerChannelTimedRequestDispatcher(registry, annotatedElement, delegate);

		//WHEN
		//THEN
		testClass.dispatch(resource, context);
	}
}
