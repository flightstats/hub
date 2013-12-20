package com.flightstats.datahub.service;

import com.flightstats.datahub.model.ChannelConfiguration;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChannelHypermediaLinkBuilderTest {

	private static final String BASE_URL = "http://path.to:8080/";
	private static final String CHANNEL_URL = BASE_URL + "channel";
	private ChannelConfiguration channelConfig;

	@Before
	public void setup() {
		channelConfig = ChannelConfiguration.builder().withName("spoon").build();
	}

	@Test
	public void testBuildChannelUri() throws Exception {
		URI expected = URI.create(CHANNEL_URL + "/spoon");
		UriInfo uriInfo = mock(UriInfo.class);

		when(uriInfo.getRequestUri()).thenReturn(URI.create(CHANNEL_URL));
		when(uriInfo.getBaseUri()).thenReturn(URI.create(BASE_URL));

		ChannelHypermediaLinkBuilder testClass = new ChannelHypermediaLinkBuilder();
		URI result = testClass.buildChannelUri(channelConfig, uriInfo);
		assertEquals(expected, result);
	}

	@Test
	public void testBuildLatestUri() throws Exception {
		URI expected = URI.create(CHANNEL_URL + "/spoon/latest");
		UriInfo uriInfo = mock(UriInfo.class);

		when(uriInfo.getRequestUri()).thenReturn(URI.create(CHANNEL_URL + "/spoon"));

		ChannelHypermediaLinkBuilder testClass = new ChannelHypermediaLinkBuilder();
		URI result = testClass.buildLatestUri(uriInfo);
		assertEquals(expected, result);
	}

	@Test
	public void testBuildLatestUriForChannel() throws Exception {
		String channelName = "spoon";
		URI expected = URI.create(CHANNEL_URL + "/spoon/latest");
		UriInfo uriInfo = mock(UriInfo.class);

		when(uriInfo.getRequestUri()).thenReturn(URI.create(CHANNEL_URL));

		ChannelHypermediaLinkBuilder testClass = new ChannelHypermediaLinkBuilder();
		URI result = testClass.buildLatestUri(channelName, uriInfo);
		assertEquals(expected, result);
	}

	@Test
	public void testBuildWsLink() throws Exception {
		UriInfo uriInfo = mock(UriInfo.class);
		when(uriInfo.getRequestUri()).thenReturn(URI.create(CHANNEL_URL + "/" + channelConfig.getName()));
		ChannelHypermediaLinkBuilder testClass = new ChannelHypermediaLinkBuilder();
		URI result = testClass.buildWsLinkFor(uriInfo);
		assertEquals(URI.create("ws://path.to:8080/channel" + "/" +
				channelConfig.getName() + "/ws"), result);
	}

	@Test
	public void testBuildWsLinkForChannel() throws Exception {
		UriInfo uriInfo = mock(UriInfo.class);
		String channelName = channelConfig.getName();
		when(uriInfo.getRequestUri()).thenReturn(URI.create(CHANNEL_URL));
		ChannelHypermediaLinkBuilder testClass = new ChannelHypermediaLinkBuilder();
		URI result = testClass.buildWsLinkFor(channelName, uriInfo);
		assertEquals(URI.create("ws://path.to:8080/channel" + "/" +
				channelName + "/ws"), result);
	}
}
