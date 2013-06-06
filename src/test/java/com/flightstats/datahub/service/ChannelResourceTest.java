package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.ChannelCreationRequest;
import com.flightstats.datahub.model.exception.AlreadyExistsException;
import com.flightstats.datahub.model.exception.InvalidRequestException;
import com.flightstats.rest.HalLink;
import com.flightstats.rest.Linked;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class ChannelResourceTest {

	@Test
	public void testChannelCreation() throws Exception {
		String channelName = "UHF";

		ChannelCreationRequest channelCreationRequest = new ChannelCreationRequest(channelName);
		Date date = new Date();
		ChannelConfiguration channelConfiguration = new ChannelConfiguration(channelName, date);
		String channelUri = "http://path/to/UHF";
		String latestUri = "http://path/to/UHF/latest";
		String wsUri = "ws://path/to/UHF/ws";
		Linked<ChannelConfiguration> expected = Linked.linked(channelConfiguration)
													  .withLink("self", channelUri)
													  .withLink("latest", latestUri)
													  .withLink("ws", wsUri)
													  .build();
		UriInfo uriInfo = mock(UriInfo.class);
		ChannelDao dao = mock(ChannelDao.class);
		CreateChannelValidator createChannelValidator = mock(CreateChannelValidator.class);
		ChannelHypermediaLinkBuilder linkBuilder = mock(ChannelHypermediaLinkBuilder.class);

		when(uriInfo.getRequestUri()).thenReturn(URI.create("http://path/to"));
		when(dao.channelExists(channelName)).thenReturn(false);
		when(dao.createChannel(channelName)).thenReturn(channelConfiguration);
		when(linkBuilder.buildChannelUri(channelConfiguration)).thenReturn(URI.create(channelUri));
		when(linkBuilder.buildLatestUri(channelName)).thenReturn(URI.create(latestUri));
		when(linkBuilder.buildWsLinkFor(channelName)).thenReturn(URI.create(wsUri));

		ChannelResource testClass = new ChannelResource(dao, linkBuilder, createChannelValidator);

		Response response = testClass.createChannel(channelCreationRequest);

		verify(dao).createChannel(channelName);

		assertEquals(201, response.getStatus());
		assertEquals(new URI(channelUri), response.getMetadata().getFirst("location"));
		assertEquals(expected, response.getEntity());
	}

	@Test(expected = InvalidRequestException.class)
	public void testChannelCreation_emptyChannelName() throws Exception {
		String channelName = "  ";

		ChannelCreationRequest channelCreationRequest = new ChannelCreationRequest(channelName);
		ChannelDao dao = mock(ChannelDao.class);
		CreateChannelValidator createChannelValidator = new CreateChannelValidator(dao);
		ChannelHypermediaLinkBuilder linkBuilder = mock(ChannelHypermediaLinkBuilder.class);

		ChannelResource testClass = new ChannelResource(dao, linkBuilder, createChannelValidator);

		testClass.createChannel(channelCreationRequest);
	}

	@Test(expected = AlreadyExistsException.class)
	public void testChannelCreation_channelAlreadyExists() throws Exception {
		String channelName = "zippy";

		ChannelCreationRequest channelCreationRequest = new ChannelCreationRequest(channelName);
		ChannelDao dao = mock(ChannelDao.class);
		CreateChannelValidator createChannelValidator = new CreateChannelValidator(dao);
		ChannelHypermediaLinkBuilder linkBuilder = mock(ChannelHypermediaLinkBuilder.class);

		ChannelResource testClass = new ChannelResource(dao, linkBuilder, createChannelValidator);
		when(dao.channelExists(any(String.class))).thenReturn(true);
		testClass.createChannel(channelCreationRequest);
	}

	@Test
	public void testGetChannels() throws Exception {
		//GIVEN
		ChannelConfiguration channel1 = new ChannelConfiguration("foo", null);
		ChannelConfiguration channel2 = new ChannelConfiguration("bar", null);
		Iterable<ChannelConfiguration> channels = Arrays.asList(channel1, channel2);
		String channel1Uri = "http://superfoo";
		String channel2Uri = "http://superbar";

		ChannelDao dao = mock(ChannelDao.class);
		ChannelHypermediaLinkBuilder linkBuilder = mock(ChannelHypermediaLinkBuilder.class);

		ChannelResource testClass = new ChannelResource(dao, linkBuilder, mock(CreateChannelValidator.class));

		//WHEN
		when(dao.getChannels()).thenReturn(channels);
		when(linkBuilder.buildChannelUri(channel1.getName())).thenReturn(URI.create(channel1Uri));
		when(linkBuilder.buildChannelUri(channel2.getName())).thenReturn(URI.create(channel2Uri));

		Response result = testClass.getChannels();

		//THEN

		List<HalLink> links = ((Linked<Map>) result.getEntity()).getHalLinks().getLinks();
		assertNull(((Linked<Map>) result.getEntity()).getObject());
		assertEquals(2, links.size());
		assertEquals(new HalLink(channel1.getName(), URI.create(channel1Uri)), links.get(0));
		assertEquals(new HalLink(channel2.getName(), URI.create(channel2Uri)), links.get(1));
	}
}
