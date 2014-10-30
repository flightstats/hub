package com.flightstats.hub.service;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ContentKey;

import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static org.mockito.Mockito.mock;

public class SingleChannelResourceTest {

    private String channelName;
    private String contentType;
    private String contentLanguage;
    private URI channelUri;
    private ChannelLinkBuilder linkBuilder;
    private ContentKey contentKey;
    private URI itemUri;
    private UriInfo uriInfo;
    private ChannelService channelService = mock(ChannelService.class);

    private int DEFAULT_MAX_PAYLOAD = 1024 * 1024 * 10;

    //todo - gfm - 10/28/14 -
    /*@Before
	public void setup() throws Exception {
		channelName = "UHF";
		contentType = "text/plain";
		contentLanguage = "en";
		channelUri = URI.create("http://testification.com/channel/UHF");
		URI requestUri = URI.create("http://testification.com/channel/UHF");
		itemUri = URI.create("http://testification.com/channel/UHF/1200");
		contentKey = new ContentKey(1200);
		linkBuilder = new ChannelLinkBuilder();
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
		InsertedContentKey expectedResponse = new InsertedContentKey(contentKey, null);

        Content content = Content.builder()
                .withData(data)
                .withContentLanguage(contentLanguage)
                .withContentType(contentType)
                .build();
        when(channelService.insert(channelName, content)).thenReturn(new InsertedContentKey(contentKey, null));

        SingleChannelResource testClass = new SingleChannelResource(channelService, linkBuilder, DEFAULT_MAX_PAYLOAD, uriInfo);
		Response response = testClass.insertValue(channelName, contentType, contentLanguage, null, data);

		assertEquals(response.getStatus(), Response.Status.CREATED.getStatusCode());

		Linked<InsertedContentKey> result = (Linked<InsertedContentKey>) response.getEntity();

        assertThat(result.getHalLinks().getLinks(), hasItems(selfLink, channelLink));
		InsertedContentKey insertionResult = result.getObject();

		assertEquals(expectedResponse.getKey(), insertionResult.getKey());
		assertEquals(itemUri, response.getMetadata().getFirst("Location"));
	}

    @Test
    public void testInsert_channelExistenceNotCached() throws Exception {
        InsertedContentKey result = new InsertedContentKey(new ContentKey(1000), null);
        byte[] data = "SomeData".getBytes();
        SingleChannelResource testClass = new SingleChannelResource(channelService, linkBuilder, DEFAULT_MAX_PAYLOAD, uriInfo);

        Content content = Content.builder().withData(data).withContentLanguage(contentLanguage).withContentType(contentType).build();
        when(channelService.insert(channelName,  content)).thenReturn(result);

        testClass.insertValue(channelName, contentType, contentLanguage, null, data);

        verify(channelService).channelExists(channelName);
    }

    @Test(expected = WebApplicationException.class)
    public void testInsert_channelExistenceNotCached_channelDoesntExist() throws Exception {
        InsertedContentKey result = new InsertedContentKey(null, null);
        byte[] data = "SomeData".getBytes();
        when(channelService.channelExists(channelName)).thenReturn(false);
        SingleChannelResource testClass = new SingleChannelResource(channelService, linkBuilder, DEFAULT_MAX_PAYLOAD, uriInfo);

        Content content = Content.builder().withData(data).withContentLanguage(contentLanguage).withContentType(contentType).build();
        when(channelService.insert(channelName,  content)).thenReturn(result);

        testClass.insertValue(channelName, contentType, contentLanguage, null, data);
    }


    @Test
    public void testInsert_payloadSizeGreaterThanMaxSizeReturns413() throws Exception {
        byte[] data = new byte[1025];
        new Random().nextBytes(data);
        SingleChannelResource testClass = new SingleChannelResource(channelService, linkBuilder, 1024, uriInfo);

        Response result = testClass.insertValue(channelName, contentType, contentLanguage, null, data);

        assertEquals(413, result.getStatus());
        assertEquals("Max payload size is 1024 bytes.", result.getEntity().toString());
    }*/

}
