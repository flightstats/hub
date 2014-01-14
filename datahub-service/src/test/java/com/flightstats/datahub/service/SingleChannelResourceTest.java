package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelService;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.ContentKey;
import com.flightstats.datahub.model.SequenceContentKey;
import com.flightstats.datahub.model.ValueInsertionResult;
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

import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.*;
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
	private UriInfo uriInfo;
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
		uriInfo = mock(UriInfo.class);

		when(uriInfo.getRequestUri()).thenReturn(requestUri);
        when(uriInfo.getBaseUri()).thenReturn(URI.create("http://testification.com/"));
		when(channelService.channelExists(channelName)).thenReturn(true);
	}

	@Test
	public void testGetChannelMetadataForUnknownChannel() throws Exception {

		SingleChannelResource testClass = new SingleChannelResource(channelService, linkBuilder, DEFAULT_MAX_PAYLOAD, uriInfo);
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
		ValueInsertionResult expectedResponse = new ValueInsertionResult(contentKey, null);

		when(channelService.insert(channelName, Optional.of(contentType), Optional.of(contentLanguage), data)).thenReturn(new ValueInsertionResult(contentKey,
                null));

		SingleChannelResource testClass = new SingleChannelResource(channelService, linkBuilder, DEFAULT_MAX_PAYLOAD, uriInfo);
		Response response = testClass.insertValue(channelName, contentType, contentLanguage, data);

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
        SingleChannelResource testClass = new SingleChannelResource(channelService, linkBuilder, DEFAULT_MAX_PAYLOAD, uriInfo);

        //WHEN
        when(channelService.insert(channelName, Optional.of(contentType), Optional.of(contentLanguage), data)).thenReturn(result);

        testClass.insertValue(channelName, contentType, contentLanguage, data);

        //THEN
        verify(channelService).channelExists(channelName);
    }

    @Test(expected = WebApplicationException.class)
    public void testInsert_channelExistenceNotCached_channelDoesntExist() throws Exception {
        //GIVEN
        ValueInsertionResult result = new ValueInsertionResult(null, null);
        byte[] data = "SomeData".getBytes();
        when(channelService.channelExists(channelName)).thenReturn(false);
        SingleChannelResource testClass = new SingleChannelResource(channelService, linkBuilder, DEFAULT_MAX_PAYLOAD, uriInfo);

        //WHEN
        when(channelService.insert(channelName, Optional.of(contentType), Optional.of(contentLanguage), data)).thenReturn(result);

        testClass.insertValue(channelName, contentType, contentLanguage, data);
    }


    @Test
    public void testInsert_payloadSizeGreaterThanMaxSizeReturns413() throws Exception {
        //GIVEN
        byte[] data = new byte[1025];
        new Random().nextBytes(data);
        SingleChannelResource testClass = new SingleChannelResource(channelService, linkBuilder, 1024, uriInfo);

        //WHEN
        Response result = testClass.insertValue(channelName, contentType, contentLanguage, data);

        //THEN
        assertEquals(413, result.getStatus());
        assertEquals("Max payload size is 1024 bytes.", result.getEntity().toString());
    }

}
