package com.flightstats.datahub.app.config.metrics;

import com.codahale.metrics.MetricRegistry;
import com.sun.jersey.spi.container.ResourceMethodDispatchProvider;
import com.sun.jersey.spi.dispatch.RequestDispatcher;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.mock;

public class PerChannelTimedMethodDispatchAdapterTest {

	@Test
	public void testAdapt() throws Exception {
		//GIVEN
		MetricRegistry registry = mock(MetricRegistry.class);
		PerChannelTimedMethodDispatchAdapter adapter = new PerChannelTimedMethodDispatchAdapter(registry);

		//WHEN
		RequestDispatcher result = adapter.adapt(mock(ResourceMethodDispatchProvider.class)).create(null);

		//THEN
		assertTrue(result instanceof PerChannelTimedRequestDispatcher);
	}

}
