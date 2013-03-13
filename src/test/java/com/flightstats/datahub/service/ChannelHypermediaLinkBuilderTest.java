package com.flightstats.datahub.service;

import com.flightstats.datahub.model.ChannelConfiguration;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChannelHypermediaLinkBuilderTest {

	public static final String CHANNEL_URL = "http://path.to/channel";
	private ChannelConfiguration channelConfig;

	@Before
	public void setup() {
		channelConfig = new ChannelConfiguration("spoon", null, null);
	}

	@Test
	public void testBuildChannelUri() throws Exception {
		URI expected = URI.create(CHANNEL_URL + "/spoon");
		UriInfo uriInfo = mock(UriInfo.class);

		when(uriInfo.getRequestUri()).thenReturn(URI.create(CHANNEL_URL));

		ChannelHypermediaLinkBuilder testClass = new ChannelHypermediaLinkBuilder(uriInfo, null);
		URI result = testClass.buildChannelUri(channelConfig);
		assertEquals(expected, result);
	}

	@Test
	public void testBuildLatestUri() throws Exception {
		URI expected = URI.create(CHANNEL_URL + "/spoon/latest");
		UriInfo uriInfo = mock(UriInfo.class);

		when(uriInfo.getRequestUri()).thenReturn(URI.create(CHANNEL_URL));

		ChannelHypermediaLinkBuilder testClass = new ChannelHypermediaLinkBuilder(uriInfo, null);
		URI result = testClass.buildLatestUri(channelConfig);
		assertEquals(expected, result);
	}
}
