package com.flightstats.hub.service;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.Content;
import org.junit.Before;

import javax.ws.rs.core.UriInfo;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChannelContentResourceTest {

    private ChannelService channelService;
    private Content content;
    private UriInfo uriInfo;

    @Before
    public void setUp() throws Exception {
        channelService = mock(ChannelService.class);
        uriInfo = mock(UriInfo.class);
        when(uriInfo.getRequestUri()).thenReturn(null);
        content = Content.builder().withData("found it!".getBytes()).withMillis(0).build();

    }

    /*@Test
    public void testGetValue() throws Exception {
        String channelName = "canal4";
        byte[] expected = new byte[]{55, 66, 77, 88};
        ContentKey key = new ContentKey( 1000);
        Content content = Content.builder().withData(expected).withContentType("text/plain").withContentLanguage("en").withMillis(0L).build();
        LinkedContent linkedValue = new LinkedContent(content);

        Request request = Request.builder().channel(channelName).id(key.keyToString()).build();
        when(channelService.getValue(request)).thenReturn(Optional.of(linkedValue));

        ChannelContentResource testClass = new ChannelContentResource(uriInfo, channelService);
        Response result = testClass.getValue(channelName, key.keyToString(), null, "");

        assertEquals(MediaType.TEXT_PLAIN_TYPE, result.getMetadata().getFirst("Content-Type"));
        assertEquals("en", result.getMetadata().getFirst("Content-Language"));
        assertEquals(expected, result.getEntity());
    }

    @Test
    public void testGetValueNoContentTypeSpecified() throws Exception {
        String channelName = "canal4";
        ContentKey key = new ContentKey( 1000);
        byte[] expected = new byte[]{55, 66, 77, 88};
        Content content = Content.builder().withData(expected).build();
        Request request = Request.builder().channel(channelName).id(key.keyToString()).build();
        when(channelService.getValue(request)).thenReturn(Optional.of(new LinkedContent(content)));

        ChannelContentResource testClass = new ChannelContentResource(uriInfo, channelService);
        Response result = testClass.getValue(channelName, key.keyToString(), null, "");

        assertEquals(MediaType.APPLICATION_OCTET_STREAM_TYPE, result.getMetadata().getFirst("Content-Type"));      //null, and the framework defaults to application/octet-stream
        assertEquals(expected, result.getEntity());
    }

    @Test
    public void testGetValueContentMismatch() throws Exception {
        String channelName = "canal4";
        ContentKey key = new ContentKey( 1000);
        byte[] expected = new byte[]{55, 66, 77, 88};
        Content content = Content.builder().withData(expected).withContentType(MediaType.APPLICATION_XML).build();
        Request request = Request.builder().channel(channelName).id(key.keyToString()).build();
        when(channelService.getValue(request)).thenReturn(Optional.of(new LinkedContent(content)));

        ChannelContentResource testClass = new ChannelContentResource(uriInfo, channelService);
        Response result = testClass.getValue(channelName, key.keyToString(), MediaType.APPLICATION_JSON, "");

        assertEquals(406, result.getStatus());
    }

    @Test
    public void testGetValueNotFound() throws Exception {

        String channelName = "canal4";
        ContentKey key = new ContentKey( 1000);
        Request request = Request.builder().channel(channelName).id(key.keyToString()).build();
        when(channelService.getValue(request)).thenReturn(Optional.<LinkedContent>absent());

        ChannelContentResource testClass = new ChannelContentResource(uriInfo, channelService);
        try {
            testClass.getValue(channelName, key.keyToString(), null, "");
            fail("Should have thrown exception.");
        } catch (WebApplicationException e) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), e.getResponse().getStatus());
        }
    }

    @Test
    public void testCreationDateHeaderInResponse() throws Exception {
        String channelName = "woo";
        ContentKey key = new ContentKey(  1000);
        Content content = Content.builder().withData("found it!".getBytes()).withMillis(987654321).build();
        LinkedContent linkedValue = new LinkedContent(content);
        Request request = Request.builder().channel(channelName).id(key.keyToString()).build();
        when(channelService.getValue(request)).thenReturn(Optional.of(linkedValue));

        ChannelContentResource testClass = new ChannelContentResource(uriInfo, channelService);
        Response result = testClass.getValue(channelName, key.keyToString(), null, "");

        String creationDateString = (String) result.getMetadata().getFirst(Headers.CREATION_DATE);
        assertEquals("1970-01-12T10:20:54.321Z", creationDateString);
    }

    @Test
    public void testPreviousLink() throws Exception {
        String channelName = "woo";
        ContentKey previousKey = new ContentKey(1000);
        ContentKey key = new ContentKey(1001);
        LinkedContent linkedValue = new LinkedContent(content, previousKey, null);

        UriInfo uriInfo = mock(UriInfo.class);

        Request request = Request.builder().channel(channelName).id(key.keyToString()).build();
        when(channelService.getValue(request)).thenReturn(Optional.of(linkedValue));
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://path/to/thisitem123"));

        ChannelContentResource testClass = new ChannelContentResource(uriInfo, channelService);
        Response result = testClass.getValue(channelName, key.keyToString(), null, "");

        String link = (String) result.getMetadata().getFirst("Link");
        assertEquals("<http://path/to/1000>;rel=\"previous\"", link);
    }

    @Test
    public void testPreviousLink_none() throws Exception {
        String channelName = "woo";
        ContentKey key = new ContentKey( 1000);
        LinkedContent linkedValue = new LinkedContent(content);

        Request request = Request.builder().channel(channelName).id(key.keyToString()).build();
        when(channelService.getValue(request)).thenReturn(Optional.of(linkedValue));

        ChannelContentResource testClass = new ChannelContentResource(uriInfo, channelService);
        Response result = testClass.getValue(channelName, key.keyToString(), null, "");

        String link = (String) result.getMetadata().getFirst("Link");
        assertNull(link);
    }

    @Test
    public void testNextLink() throws Exception {
        String channelName = "nerxt";
        ContentKey nextKey = new ContentKey( 1001);
        ContentKey key = new ContentKey( 1000);
        LinkedContent linkedValue = new LinkedContent(content, null, nextKey);

        UriInfo uriInfo = mock(UriInfo.class);
        Request request = Request.builder().channel(channelName).id(key.keyToString()).build();
        when(channelService.getValue(request)).thenReturn(Optional.of(linkedValue));
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://path/to/thisitem123"));

        ChannelContentResource testClass = new ChannelContentResource(uriInfo, channelService);
        Response result = testClass.getValue(channelName, key.keyToString(), null, "");

        String link = (String) result.getMetadata().getFirst("Link");
        assertEquals("<http://path/to/1001>;rel=\"next\"", link);
    }

    @Test
    public void testNextLinkNone() throws Exception {
        String channelName = "nerxt";
        ContentKey key = new ContentKey( 1000);
        LinkedContent linkedValue = new LinkedContent(content);
        Request request = Request.builder().channel(channelName).id(key.keyToString()).build();
        when(channelService.getValue(request)).thenReturn(Optional.of(linkedValue));

        ChannelContentResource testClass = new ChannelContentResource(uriInfo, channelService);
        Response result = testClass.getValue(channelName, key.keyToString(), null, "");

        String link = (String) result.getMetadata().getFirst("Link");
        assertNull(link);
    }

    @Test
    public void testLanguageHeader_missing() throws Exception {
        String channelName = "canal4";
        ContentKey key = new ContentKey( 1000);
        LinkedContent linkedValue = new LinkedContent(content);
        Request request = Request.builder().channel(channelName).id(key.keyToString()).build();
        when(channelService.getValue(request)).thenReturn(Optional.of(linkedValue));

        ChannelContentResource testClass = new ChannelContentResource(uriInfo, channelService);
        Response result = testClass.getValue(channelName, key.keyToString(), null, "");

        assertNull(result.getMetadata().getFirst("Content-Language"));
    }

    @Test
    public void testEncodingHeader_missing() throws Exception {
        String channelName = "canal4";
        ContentKey key = new ContentKey( 1000);
        LinkedContent linkedValue = new LinkedContent(content);

        Request request = Request.builder().channel(channelName).id(key.keyToString()).build();
        when(channelService.getValue(request)).thenReturn(Optional.of(linkedValue));

        ChannelContentResource testClass = new ChannelContentResource(uriInfo, channelService);
        Response result = testClass.getValue(channelName, key.keyToString(), null, "");

        assertNull(result.getMetadata().getFirst("Content-Encoding"));
    }*/
}
