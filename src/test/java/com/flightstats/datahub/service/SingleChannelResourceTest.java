package com.flightstats.datahub.service;

import com.flightstats.datahub.cluster.ReentrantChannelLockFactory;
import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.*;
import com.flightstats.datahub.model.exception.NoSuchChannelException;
import com.flightstats.rest.HalLink;
import com.flightstats.rest.Linked;
import com.google.common.base.Optional;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Date;
import java.util.concurrent.Callable;

import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class SingleChannelResourceTest {

	private String channelName;
	private String contentType;
	private String contentEncoding;
	private String contentLanguage;
	private URI channelUri;
	private ChannelDao dao;
	private ChannelHypermediaLinkBuilder linkBuilder;
	public static final Date CREATION_DATE = new Date(12345L);
	private ChannelConfiguration channelConfig;
	private DataHubKey dataHubKey;
	private URI itemUri;
	private ChannelLockExecutor channelLockExecutor;
	private HazelcastInstance hazelcast;
	private ITopic<Object> topic;

	@Before
	public void setup() {
		channelName = "UHF";
		contentType = "text/plain";
		contentEncoding = "gzip";
		contentLanguage = "en";
		channelUri = URI.create("http://testification.com/channel/spoon");
		URI requestUri = URI.create("http://testification.com/channel/spoon");
		URI latestUri = URI.create("http://testification.com/channel/spoon/latest");
		itemUri = URI.create("http://testification.com/channel/spoon/888item888");
		dataHubKey = new DataHubKey(CREATION_DATE, (short) 12);
		channelConfig = new ChannelConfiguration(channelName, CREATION_DATE, null);

		UriInfo urlInfo = mock(UriInfo.class);
		dao = mock(ChannelDao.class);
		linkBuilder = mock(ChannelHypermediaLinkBuilder.class);
		channelLockExecutor = mock(ChannelLockExecutor.class);
		hazelcast = mock(HazelcastInstance.class);
		topic = mock(ITopic.class);

		when(urlInfo.getRequestUri()).thenReturn(requestUri);
		when(dao.channelExists(channelName)).thenReturn(true);
		when(linkBuilder.buildChannelUri(channelConfig)).thenReturn(channelUri);
		when(linkBuilder.buildChannelUri(channelName)).thenReturn(channelUri);
		when(linkBuilder.buildLatestUri()).thenReturn(latestUri);
		when(linkBuilder.buildItemUri(dataHubKey)).thenReturn(itemUri);
		when(hazelcast.getTopic("ws:" + channelName)).thenReturn(topic);
	}

	@Test
	public void testGetChannelMetadataForKnownChannel() throws Exception {

		UriInfo uriInfo = mock(UriInfo.class);
		DataHubKey key = new DataHubKey(new Date(21), (short) 0);
		when(dao.channelExists(anyString())).thenReturn(true);
		when(dao.getChannelConfiguration(channelName)).thenReturn(channelConfig);
		when(dao.findLastUpdatedKey(channelName)).thenReturn(Optional.of(key));
		when(uriInfo.getRequestUri()).thenReturn(channelUri);

		SingleChannelResource testClass = new SingleChannelResource(dao, linkBuilder, null, null);

		Linked<MetadataResponse> result = testClass.getChannelMetadata(channelName);
		MetadataResponse expectedResponse = new MetadataResponse(channelConfig, key.getDate());
		assertEquals(expectedResponse, result.getObject());
		HalLink selfLink = result.getHalLinks().getLinks().get(0);
		HalLink latestLink = result.getHalLinks().getLinks().get(1);
		assertEquals(new HalLink("self", channelUri), selfLink);
		assertEquals(new HalLink("latest", URI.create(channelUri.toString() + "/latest")), latestLink);
	}

	@Test
	public void testUpdateChannelMetadataWithNonNullTtl() throws Exception {

		UriInfo uriInfo = mock(UriInfo.class);
		when(dao.channelExists(anyString())).thenReturn(true);
		when(dao.getChannelConfiguration(channelName)).thenReturn(channelConfig);
		when(uriInfo.getRequestUri()).thenReturn(channelUri);

		ChannelUpdateRequest request = ChannelUpdateRequest.builder().withTtlMillis(30000L).build();
		ChannelConfiguration newConfig = ChannelConfiguration.builder().withChannelConfiguration(channelConfig).withTtlMillis(30000L).build();
		Response expectedResponse = Response.ok().entity(linkBuilder.buildLinkedChannelConfig(newConfig, channelUri)).build();

		SingleChannelResource testClass = new SingleChannelResource(dao, linkBuilder, null, null);
		Response result = testClass.updateMetadata(request, channelName);

		assertEquals(expectedResponse.getEntity(), result.getEntity());
	}

	@Test
	public void testUpdateChannelMetadataWithNullTtl() throws Exception {

		UriInfo uriInfo = mock(UriInfo.class);
		when(dao.channelExists(anyString())).thenReturn(true);
		when(dao.getChannelConfiguration(channelName)).thenReturn(channelConfig);
		when(uriInfo.getRequestUri()).thenReturn(channelUri);

		ChannelUpdateRequest request = ChannelUpdateRequest.builder().withTtlMillis(null).build();
		ChannelConfiguration newConfig = ChannelConfiguration.builder().withChannelConfiguration(channelConfig).withTtlMillis(null).build();
		Response expectedResponse = Response.ok().entity(linkBuilder.buildLinkedChannelConfig(newConfig, channelUri)).build();

		SingleChannelResource testClass = new SingleChannelResource(dao, linkBuilder, null, null);
		Response result = testClass.updateMetadata(request, channelName);

		assertEquals(expectedResponse.getEntity(), result.getEntity());
	}

	@Test(expected = WebApplicationException.class)
	public void testUpdateChannelMetadataForUnknownChannel() throws Exception {

		ChannelUpdateRequest request = ChannelUpdateRequest.builder().withTtlMillis(30000L).build();

		when(dao.channelExists(anyString())).thenReturn(false);

		SingleChannelResource testClass = new SingleChannelResource(dao, null, null, null);
		testClass.updateMetadata(request, channelName);
	}

	@Test
	public void testGetChannelMetadataForUnknownChannel() throws Exception {
		when(dao.channelExists("unknownChannel")).thenReturn(false);

		SingleChannelResource testClass = new SingleChannelResource(dao, linkBuilder, null, null);
		try {
			testClass.getChannelMetadata("unknownChannel");
			fail("Should have thrown a 404");
		} catch (WebApplicationException e) {
			Response response = e.getResponse();
			assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
		}
	}

	@Test
	public void testInsertValue() throws Exception {
		byte[] data = new byte[]{'b', 'o', 'l', 'o', 'g', 'n', 'a'};

		HalLink selfLink = new HalLink("self", itemUri);
		HalLink channelLink = new HalLink("channel", channelUri);
		ValueInsertionResult expectedResponse = new ValueInsertionResult(dataHubKey);
		channelLockExecutor = new ChannelLockExecutor(new ReentrantChannelLockFactory());

		when(dao.insert(channelName, Optional.of(contentType), Optional.of(contentEncoding), Optional.of(contentLanguage), data)).thenReturn(new ValueInsertionResult(dataHubKey));

		SingleChannelResource testClass = new SingleChannelResource(dao, linkBuilder, channelLockExecutor, hazelcast);
		Response response = testClass.insertValue(channelName, contentType, contentEncoding, contentLanguage, data);

		assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());

		Linked<ValueInsertionResult> result = (Linked<ValueInsertionResult>) response.getEntity();

		assertThat(result.getHalLinks().getLinks(), hasItems(selfLink, channelLink));
		ValueInsertionResult insertionResult = result.getObject();

		assertEquals(expectedResponse, insertionResult);
		assertEquals(itemUri, response.getMetadata().getFirst("Location"));
	}

	@Test
	public void testInsertPostsToTopic() throws Exception {
		byte[] data = new byte[]{'b', 'o', 'l', 'o', 'g', 'n', 'a'};

		ValueInsertionResult result = new ValueInsertionResult(dataHubKey);
		channelLockExecutor = new ChannelLockExecutor(new ReentrantChannelLockFactory());

		when(dao.insert(channelName, Optional.of(contentType), Optional.of(contentEncoding), Optional.of(contentLanguage), data)).thenReturn(result);

		SingleChannelResource testClass = new SingleChannelResource(dao, linkBuilder, channelLockExecutor, hazelcast);
		testClass.insertValue(channelName, contentType, contentEncoding, contentLanguage, data);
		verify(topic).publish(itemUri);
	}

	@Test(expected = NoSuchChannelException.class)
	public void testInsertValue_unknownChannel() throws Exception {
		byte[] data = new byte[]{'b', 'o', 'l', 'o', 'g', 'n', 'a'};

		ChannelDao dao = mock(ChannelDao.class);

		when(dao.channelExists(anyString())).thenReturn(false);
		when(channelLockExecutor.execute(eq(channelName), any(Callable.class))).thenThrow(
				new NoSuchChannelException("No such channel: " + channelName, new RuntimeException()));

		SingleChannelResource testClass = new SingleChannelResource(dao, linkBuilder, channelLockExecutor, null);
		testClass.insertValue(channelName, contentType, contentEncoding, contentLanguage, data);
	}
}
