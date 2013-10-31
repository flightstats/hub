package com.flightstats.datahub.service;

import com.flightstats.datahub.model.*;
import com.flightstats.rest.HalLink;
import com.flightstats.rest.Linked;
import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.Callable;

import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.*;

public class SingleChannelResourceTest {

	private String channelName;
	private String contentType;
	private String contentLanguage;
	private URI channelUri;
	private ChannelHypermediaLinkBuilder linkBuilder;
	public static final Date CREATION_DATE = new Date(12345L);
	private ChannelConfiguration channelConfig;
	private DataHubKey dataHubKey;
	private URI itemUri;
	private UriInfo urlInfo;
	private DataHubService dataHubService = mock(DataHubService.class);
    private Cache cache;

    private int DEFAULT_MAX_PAYLOAD = 1024 * 10;

	@Before
	public void setup() throws Exception {
		channelName = "UHF";
		contentType = "text/plain";
		contentLanguage = "en";
		channelUri = URI.create("http://testification.com/channel/spoon");
		URI requestUri = URI.create("http://testification.com/channel/spoon");
		URI latestUri = URI.create("http://testification.com/channel/spoon/latest");
		itemUri = URI.create("http://testification.com/channel/spoon/888item888");
		dataHubKey = new DataHubKey(CREATION_DATE, (short) 12);
		channelConfig = new ChannelConfiguration(channelName, CREATION_DATE, null);
		linkBuilder = mock(ChannelHypermediaLinkBuilder.class);
		urlInfo = mock(UriInfo.class);
        cache = mock(Cache.class);

		when(urlInfo.getRequestUri()).thenReturn(requestUri);
		when(dataHubService.channelExists(channelName)).thenReturn(true);
		when(linkBuilder.buildChannelUri(channelConfig, urlInfo)).thenReturn(channelUri);
		when(linkBuilder.buildChannelUri(channelName, urlInfo)).thenReturn(channelUri);
		when(linkBuilder.buildLatestUri(urlInfo)).thenReturn(latestUri);
		when(linkBuilder.buildItemUri(dataHubKey, requestUri)).thenReturn(itemUri);
        when(cache.get(anyObject(), any(Callable.class))).thenReturn(true);
	}

	@Test
	public void testGetChannelMetadataForKnownChannel() throws Exception {
		DataHubKey key = new DataHubKey(new Date(21), (short) 0);

		when(dataHubService.channelExists(anyString())).thenReturn(true);
		when(dataHubService.getChannelConfiguration(channelName)).thenReturn(channelConfig);
		when(dataHubService.findLastUpdatedKey(channelName)).thenReturn(Optional.of(key));
		when(urlInfo.getRequestUri()).thenReturn(channelUri);

		SingleChannelResource testClass = new SingleChannelResource(dataHubService, linkBuilder, cache, DEFAULT_MAX_PAYLOAD);

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

		SingleChannelResource testClass = new SingleChannelResource(dataHubService, linkBuilder, cache, DEFAULT_MAX_PAYLOAD);
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

		SingleChannelResource testClass = new SingleChannelResource(dataHubService, linkBuilder, cache, DEFAULT_MAX_PAYLOAD);
		Response result = testClass.updateMetadata(request, channelName, uriInfo);

		assertEquals(expectedResponse.getEntity(), result.getEntity());
	}

	@Test(expected = WebApplicationException.class)
	public void testUpdateChannelMetadataForUnknownChannel() throws Exception {

		ChannelUpdateRequest request = ChannelUpdateRequest.builder().withTtlMillis(30000L).build();

		when(cache.get(anyString(), any(Callable.class))).thenReturn(false);

		SingleChannelResource testClass = new SingleChannelResource(dataHubService, null, cache, DEFAULT_MAX_PAYLOAD);
		testClass.updateMetadata(request, channelName, urlInfo);
	}

	@Test
	public void testGetChannelMetadataForUnknownChannel() throws Exception {
        when(cache.get(eq("unknownChannel"), any(Callable.class))).thenReturn(false);

		SingleChannelResource testClass = new SingleChannelResource(dataHubService, linkBuilder, cache, DEFAULT_MAX_PAYLOAD);
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

		when(dataHubService.insert(channelName, data, Optional.of(contentType), Optional.of(contentLanguage))).thenReturn(new ValueInsertionResult(dataHubKey));
        when(cache.get(eq(channelName), any(Callable.class))).thenReturn(true);

		SingleChannelResource testClass = new SingleChannelResource(dataHubService, linkBuilder, cache, DEFAULT_MAX_PAYLOAD);
		Response response = testClass.insertValue(channelName, contentType, contentLanguage, data, urlInfo);

		assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());

		Linked<ValueInsertionResult> result = (Linked<ValueInsertionResult>) response.getEntity();

		assertThat(result.getHalLinks().getLinks(), hasItems(selfLink, channelLink));
		ValueInsertionResult insertionResult = result.getObject();

		assertEquals(expectedResponse, insertionResult);
		assertEquals(itemUri, response.getMetadata().getFirst("Location"));
	}

    @Test
    public void testInsert_channelExistenceCached() throws Exception {
        //GIVEN
        ValueInsertionResult result = new ValueInsertionResult(null);
        byte[] data = "SomeData".getBytes();
        Cache<String, Boolean> cache = mock(Cache.class);
        SingleChannelResource testClass = new SingleChannelResource(dataHubService, linkBuilder, cache, DEFAULT_MAX_PAYLOAD);

        //WHEN
        when(dataHubService.insert(channelName, data, Optional.of(contentType), Optional.of(contentLanguage))).thenReturn(result);
        when(cache.get(eq(channelName), any(Callable.class))).thenReturn(true);
        testClass.insertValue(channelName, contentType, contentLanguage, data, urlInfo);

        //THEN
        verify(dataHubService, never()).channelExists(anyString());
    }

    @Test
    public void testInsert_channelExistenceNotCached() throws Exception {
        //GIVEN
        ValueInsertionResult result = new ValueInsertionResult(null);
        byte[] data = "SomeData".getBytes();
        Cache<String, Boolean> cache = mock(Cache.class);
        SingleChannelResource testClass = new SingleChannelResource(dataHubService, linkBuilder, cache, DEFAULT_MAX_PAYLOAD);

        //WHEN
        ArgumentCaptor<Callable> callableCaptor = ArgumentCaptor.forClass(Callable.class);
        when(dataHubService.insert(channelName, data, Optional.of(contentType), Optional.of(contentLanguage))).thenReturn(result);
        when(cache.get(eq(channelName), callableCaptor.capture())).thenReturn(true);

        testClass.insertValue(channelName, contentType, contentLanguage, data, urlInfo);
        Callable callback = callableCaptor.getValue();

        Boolean exists = (Boolean) callback.call();

        //THEN
        verify(dataHubService).channelExists(channelName);
        assertFalse(exists);
    }

    @Test
    public void testInsert_payloadSizeGreaterThanMaxSizeReturns413() throws Exception {
        //GIVEN
        byte[] data = new byte[1025];
        new Random().nextBytes(data);
        SingleChannelResource testClass = new SingleChannelResource(dataHubService, linkBuilder, cache, 1024);

        //WHEN
        Response result = testClass.insertValue(channelName, contentType, contentLanguage, data, urlInfo);

        //THEN
        assertEquals(413, result.getStatus());
        assertEquals("Max payload size is 1024 bytes.", result.getEntity().toString());
    }

}
