package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.ChannelCreationRequest;
import com.flightstats.rest.Linked;
import org.junit.Test;

import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class ChannelResourceTest {

	@Test
	public void testChannelCreation() throws Exception {
		String channelName = "UHF";

		ChannelCreationRequest channelCreationRequest = new ChannelCreationRequest(channelName);
		Date date = new Date();
		ChannelConfiguration channelConfiguration = new ChannelConfiguration(channelName, date, null);
		String channelUri = "http://path/to/UHF";
		String latestUri = "http://path/to/UHF/latest";
		Linked<ChannelConfiguration> expected = Linked.linked(channelConfiguration)
													  .withLink("self", channelUri)
													  .withLink("latest", latestUri)
													  .build();
		UriInfo uriInfo = mock(UriInfo.class);
		ChannelDao dao = mock(ChannelDao.class);
		ChannelHypermediaLinkBuilder linkBuilder = mock(ChannelHypermediaLinkBuilder.class);

		when(uriInfo.getRequestUri()).thenReturn(URI.create("http://path/to"));
		when(dao.channelExists(channelName)).thenReturn(false);
		when(dao.createChannel(channelName)).thenReturn(channelConfiguration);
		when(linkBuilder.buildChannelUri(channelConfiguration)).thenReturn(URI.create(channelUri));
		when(linkBuilder.buildLatestUri(channelConfiguration)).thenReturn(URI.create(latestUri));

		ChannelResource testClass = new ChannelResource(dao, linkBuilder);

		Linked<ChannelConfiguration> result = testClass.createChannel(channelCreationRequest);

		verify(dao).createChannel(channelName);

		assertEquals(expected, result);
	}
}
