package com.flightstats.datahub.service;

import com.flightstats.datahub.model.*;
import com.flightstats.rest.HalLink;
import com.flightstats.rest.Linked;
import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Date;

import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SingleChannelResourceTest {

	private String channelName;
	private String contentType;
	private String contentEncoding;
	private String contentLanguage;
	private URI channelUri;
	private ChannelHypermediaLinkBuilder linkBuilder;
	public static final Date CREATION_DATE = new Date(12345L);
	private ChannelConfiguration channelConfig;
	private DataHubKey dataHubKey;
	private URI itemUri;
	private UriInfo urlInfo;
	private DataHubService dataHubService = mock(DataHubService.class);

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
		linkBuilder = mock(ChannelHypermediaLinkBuilder.class);
		urlInfo = mock(UriInfo.class);

		when(urlInfo.getRequestUri()).thenReturn(requestUri);
		when(dataHubService.channelExists(channelName)).thenReturn(true);
		when(linkBuilder.buildChannelUri(channelConfig, urlInfo)).thenReturn(channelUri);
		when(linkBuilder.buildChannelUri(channelName, urlInfo)).thenReturn(channelUri);
		when(linkBuilder.buildLatestUri(urlInfo)).thenReturn(latestUri);
		when(linkBuilder.buildItemUri(dataHubKey, requestUri)).thenReturn(itemUri);
	}

	@Test
	public void testGetChannelMetadataForKnownChannel() throws Exception {
		DataHubKey key = new DataHubKey(new Date(21), (short) 0);

		when(dataHubService.channelExists(anyString())).thenReturn(true);
		when(dataHubService.getChannelConfiguration(channelName)).thenReturn(channelConfig);
		when(dataHubService.findLastUpdatedKey(channelName)).thenReturn(Optional.of(key));
		when(urlInfo.getRequestUri()).thenReturn(channelUri);

		SingleChannelResource testClass = new SingleChannelResource(dataHubService, linkBuilder);

		Linked<MetadataResponse> result = testClass.getChannelMetadata(channelName, urlInfo);
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
		when(dataHubService.channelExists(anyString())).thenReturn(true);
		when(dataHubService.getChannelConfiguration(channelName)).thenReturn(channelConfig);
		when(uriInfo.getRequestUri()).thenReturn(channelUri);

		ChannelUpdateRequest request = ChannelUpdateRequest.builder().withTtlMillis(30000L).build();
		ChannelConfiguration newConfig = ChannelConfiguration.builder().withChannelConfiguration(channelConfig).withTtlMillis(30000L).build();
		Response expectedResponse = Response.ok().entity(linkBuilder.buildLinkedChannelConfig(newConfig, channelUri, uriInfo)).build();

		SingleChannelResource testClass = new SingleChannelResource(dataHubService, linkBuilder);
		Response result = testClass.updateMetadata(request, channelName, uriInfo);

		assertEquals(expectedResponse.getEntity(), result.getEntity());
	}

	@Test
	public void testUpdateChannelMetadataWithNullTtl() throws Exception {

		UriInfo uriInfo = mock(UriInfo.class);
		when(dataHubService.channelExists(anyString())).thenReturn(true);
		when(dataHubService.getChannelConfiguration(channelName)).thenReturn(channelConfig);
		when(uriInfo.getRequestUri()).thenReturn(channelUri);

		ChannelUpdateRequest request = ChannelUpdateRequest.builder().withTtlMillis(null).build();
		ChannelConfiguration newConfig = ChannelConfiguration.builder().withChannelConfiguration(channelConfig).withTtlMillis(null).build();
		Response expectedResponse = Response.ok().entity(linkBuilder.buildLinkedChannelConfig(newConfig, channelUri, uriInfo)).build();

		SingleChannelResource testClass = new SingleChannelResource(dataHubService, linkBuilder);
		Response result = testClass.updateMetadata(request, channelName, uriInfo);

		assertEquals(expectedResponse.getEntity(), result.getEntity());
	}

	@Test(expected = WebApplicationException.class)
	public void testUpdateChannelMetadataForUnknownChannel() throws Exception {

		ChannelUpdateRequest request = ChannelUpdateRequest.builder().withTtlMillis(30000L).build();

		when(dataHubService.channelExists(anyString())).thenReturn(false);

		SingleChannelResource testClass = new SingleChannelResource(dataHubService, null);
		testClass.updateMetadata(request, channelName, urlInfo);
	}

	@Test
	public void testGetChannelMetadataForUnknownChannel() throws Exception {
		when(dataHubService.channelExists("unknownChannel")).thenReturn(false);

		SingleChannelResource testClass = new SingleChannelResource(dataHubService, linkBuilder);
		try {
			testClass.getChannelMetadata("unknownChannel", urlInfo);
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

		when(dataHubService.insert(channelName, data, Optional.of(contentType), Optional.of(contentEncoding), Optional.of(contentLanguage))).thenReturn(new ValueInsertionResult(dataHubKey));

		SingleChannelResource testClass = new SingleChannelResource(dataHubService, linkBuilder);
		Response response = testClass.insertValue(channelName, contentType, contentEncoding, contentLanguage, data, urlInfo);

		assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());

		Linked<ValueInsertionResult> result = (Linked<ValueInsertionResult>) response.getEntity();

		assertThat(result.getHalLinks().getLinks(), hasItems(selfLink, channelLink));
		ValueInsertionResult insertionResult = result.getObject();

		assertEquals(expectedResponse, insertionResult);
		assertEquals(itemUri, response.getMetadata().getFirst("Location"));
	}

}
