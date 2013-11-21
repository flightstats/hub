package com.flightstats.datahub.app.config.metrics;

import com.codahale.metrics.MetricRegistry;
import com.sun.jersey.spi.container.ResourceMethodDispatchProvider;
import com.sun.jersey.spi.dispatch.RequestDispatcher;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class PerChannelThroughputMethodDispatchAdapterTest {

	@Test
	public void testAdapt() throws Exception {
		//GIVEN
		MetricRegistry registry = mock(MetricRegistry.class);
        PerChannelThroughputDispatchAdapter adapter = new PerChannelThroughputDispatchAdapter(registry);

		//WHEN
		RequestDispatcher result = adapter.adapt(mock(ResourceMethodDispatchProvider.class)).create(null);

		//THEN
		assertTrue(result instanceof PerChannelThroughputRequestDispatcher);
	}

}
