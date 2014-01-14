package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelService;
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
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class SingleChannelResourceTest {

	private String channelName;
	private String contentType;
	private String contentLanguage;
	private URI channelUri;
	private ChannelHypermediaLinkBuilder linkBuilder;
	public static final Date CREATION_DATE = new Date(12345L);
	private ChannelConfiguration channelConfig;
	private ContentKey contentKey;
	private URI itemUri;
	private UriInfo urlInfo;
	private ChannelService channelService = mock(ChannelService.class);

    private int DEFAULT_MAX_PAYLOAD = 1024 * 1024 * 10;

	@Before
	public void setup() throws Exception {
		channelName = "UHF";
		contentType = "text/plain";
		contentLanguage = "en";
		channelUri = URI.create("http://testification.com/channel/UHF");
		URI requestUri = URI.create("http://testification.com/channel/UHF");
		itemUri = URI.create("http://testification.com/channel/UHF/1200");
		contentKey = new SequenceContentKey(1200);
		channelConfig = ChannelConfiguration.builder().withName(channelName).withCreationDate(CREATION_DATE).build();
		linkBuilder = new ChannelHypermediaLinkBuilder();
		urlInfo = mock(UriInfo.class);

		when(urlInfo.getRequestUri()).thenReturn(requestUri);
        when(urlInfo.getBaseUri()).thenReturn(URI.create("http://testification.com/"));
		when(channelService.channelExists(channelName)).thenReturn(true);
	}

	@Test
	public void testGetChannelMetadataForKnownChannel() throws Exception {
		ContentKey key = new SequenceContentKey( 1000);
        Content content = new Content(Optional.<String>absent(), Optional.<String>absent(), "blah".getBytes(),
                0L);
        LinkedContent linkedContent = new LinkedContent(content,
                Optional.<ContentKey>absent(), Optional.<ContentKey>absent());

        when(channelService.channelExists(anyString())).thenReturn(true);
		when(channelService.getChannelConfiguration(channelName)).thenReturn(channelConfig);
		when(channelService.findLastUpdatedKey(channelName)).thenReturn(Optional.of(key));
		when(channelService.getValue(channelName, key.keyToString())).thenReturn(Optional.of(linkedContent));

		when(urlInfo.getRequestUri()).thenReturn(channelUri);

		SingleChannelResource testClass = new SingleChannelResource(channelService, linkBuilder, DEFAULT_MAX_PAYLOAD);

        //todo - gfm - 1/13/14 - useful?
		/*Linked<MetadataResponse> result = testClass.getChannelMetadata(channelName, urlInfo);
		MetadataResponse expectedResponse = new MetadataResponse(channelConfig);
		assertEquals(expectedResponse, result.getObject());
		HalLink selfLink = result.getHalLinks().getLinks().get(0);
		HalLink latestLink = result.getHalLinks().getLinks().get(1);
		assertEquals(new HalLink("self", channelUri), selfLink);
		assertEquals(new HalLink("latest", URI.create(channelUri.toString() + "/latest")), latestLink);*/
	}

	@Test
	public void testUpdateChannelMetadataWithNonNullTtl() throws Exception {

		when(channelService.channelExists(anyString())).thenReturn(true);
		when(channelService.getChannelConfiguration(channelName)).thenReturn(channelConfig);

		ChannelUpdateRequest request = ChannelUpdateRequest.builder().withTtlMillis(30000L).build();
		ChannelConfiguration newConfig = ChannelConfiguration.builder().withChannelConfiguration(channelConfig).withTtlMillis(30000L).build();
		Response expectedResponse = Response.ok().entity(linkBuilder.buildChannelLinks(newConfig, channelUri)).build();

		SingleChannelResource testClass = new SingleChannelResource(channelService, linkBuilder, DEFAULT_MAX_PAYLOAD);
		Response result = testClass.updateMetadata(request, channelName, urlInfo);

        System.out.println(expectedResponse);
        assertEquals(expectedResponse.getEntity(), result.getEntity());
	}

	@Test
	public void testUpdateChannelMetadataWithNullTtl() throws Exception {

		when(channelService.channelExists(anyString())).thenReturn(true);
		when(channelService.getChannelConfiguration(channelName)).thenReturn(channelConfig);

		ChannelUpdateRequest request = ChannelUpdateRequest.builder().withTtlMillis(null).build();
		ChannelConfiguration newConfig = ChannelConfiguration.builder().withChannelConfiguration(channelConfig).withTtlMillis(null).build();
		Response expectedResponse = Response.ok().entity(linkBuilder.buildChannelLinks(newConfig, channelUri)).build();

		SingleChannelResource testClass = new SingleChannelResource(channelService, linkBuilder, DEFAULT_MAX_PAYLOAD);
		Response result = testClass.updateMetadata(request, channelName, urlInfo);

		assertEquals(expectedResponse.getEntity(), result.getEntity());
	}

	@Test(expected = WebApplicationException.class)
	public void testUpdateChannelMetadataForUnknownChannel() throws Exception {

		ChannelUpdateRequest request = ChannelUpdateRequest.builder().withTtlMillis(30000L).build();
        when(channelService.channelExists(channelName)).thenReturn(false);
		SingleChannelResource testClass = new SingleChannelResource(channelService, null, DEFAULT_MAX_PAYLOAD);
		testClass.updateMetadata(request, channelName, urlInfo);
	}

    @Test
    public void testUpdateRate() throws Exception {
        when(channelService.channelExists(anyString())).thenReturn(true);
        when(channelService.getChannelConfiguration(channelName)).thenReturn(channelConfig);

        ChannelUpdateRequest request = ChannelUpdateRequest.builder()
                .withPeakRequestRate(15)
                .withContentKiloBytes(20)
                .withRateTimeUnit(TimeUnit.MINUTES)
                .build();
        ChannelConfiguration newConfig = ChannelConfiguration.builder().withChannelConfiguration(channelConfig)
                .withPeakRequestRate(15)
                .withContentKiloBytes(20)
                .build();
        Response expectedResponse = Response.ok().entity(linkBuilder.buildChannelLinks(newConfig, channelUri)).build();

        SingleChannelResource testClass = new SingleChannelResource(channelService, linkBuilder, DEFAULT_MAX_PAYLOAD);
        Response result = testClass.updateMetadata(request, channelName, urlInfo);
        Linked<ChannelConfiguration> entity = (Linked<ChannelConfiguration>) result.getEntity();
        assertEquals(expectedResponse.getEntity(), entity);
        ChannelConfiguration entityConfig = entity.getObject();
        assertEquals(newConfig.getPeakRequestRateSeconds(), entityConfig.getPeakRequestRateSeconds());
        assertEquals(newConfig.getContentSizeKB(), entityConfig.getContentSizeKB());
    }

	@Test
	public void testGetChannelMetadataForUnknownChannel() throws Exception {

		SingleChannelResource testClass = new SingleChannelResource(channelService, linkBuilder, DEFAULT_MAX_PAYLOAD);
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
		ValueInsertionResult expectedResponse = new ValueInsertionResult(contentKey, null);

		when(channelService.insert(channelName, Optional.of(contentType), Optional.of(contentLanguage), data)).thenReturn(new ValueInsertionResult(contentKey,
                null));

		SingleChannelResource testClass = new SingleChannelResource(channelService, linkBuilder, DEFAULT_MAX_PAYLOAD);
		Response response = testClass.insertValue(channelName, contentType, contentLanguage, data, urlInfo);

		assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());

		Linked<ValueInsertionResult> result = (Linked<ValueInsertionResult>) response.getEntity();

        assertThat(result.getHalLinks().getLinks(), hasItems(selfLink, channelLink));
		ValueInsertionResult insertionResult = result.getObject();

		assertEquals(expectedResponse, insertionResult);
		assertEquals(itemUri, response.getMetadata().getFirst("Location"));
	}

    @Test
    public void testInsert_channelExistenceNotCached() throws Exception {
        //GIVEN
        ValueInsertionResult result = new ValueInsertionResult(new SequenceContentKey(1000), null);
        byte[] data = "SomeData".getBytes();
        SingleChannelResource testClass = new SingleChannelResource(channelService, linkBuilder, DEFAULT_MAX_PAYLOAD);

        //WHEN
        when(channelService.insert(channelName, Optional.of(contentType), Optional.of(contentLanguage), data)).thenReturn(result);

        testClass.insertValue(channelName, contentType, contentLanguage, data, urlInfo);

        //THEN
        verify(channelService).channelExists(channelName);
    }

    @Test(expected = WebApplicationException.class)
    public void testInsert_channelExistenceNotCached_channelDoesntExist() throws Exception {
        //GIVEN
        ValueInsertionResult result = new ValueInsertionResult(null, null);
        byte[] data = "SomeData".getBytes();
        when(channelService.channelExists(channelName)).thenReturn(false);
        SingleChannelResource testClass = new SingleChannelResource(channelService, linkBuilder, DEFAULT_MAX_PAYLOAD);

        //WHEN
        when(channelService.insert(channelName, Optional.of(contentType), Optional.of(contentLanguage), data)).thenReturn(result);

        testClass.insertValue(channelName, contentType, contentLanguage, data, urlInfo);
    }


    @Test
    public void testInsert_payloadSizeGreaterThanMaxSizeReturns413() throws Exception {
        //GIVEN
        byte[] data = new byte[1025];
        new Random().nextBytes(data);
        SingleChannelResource testClass = new SingleChannelResource(channelService, linkBuilder, 1024);

        //WHEN
        Response result = testClass.insertValue(channelName, contentType, contentLanguage, data, urlInfo);

        //THEN
        assertEquals(413, result.getStatus());
        assertEquals("Max payload size is 1024 bytes.", result.getEntity().toString());
    }

}
