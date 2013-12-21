package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelService;
import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.LinkedDataHubCompositeValue;
import com.flightstats.datahub.model.SequenceDataHubKey;
import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChannelContentResourceTest {

    private ChannelService channelService;

    @Before
    public void setUp() throws Exception {
        channelService = mock(ChannelService.class);
    }

    @Test
    public void testGetValue() throws Exception {
        String channelName = "canal4";
        byte[] expected = new byte[]{55, 66, 77, 88};
        Optional<String> contentType = Optional.of("text/plain");
        Optional<String> contentLanguage = Optional.of("en");
        DataHubKey key = new SequenceDataHubKey( 1000);
        DataHubCompositeValue value = new DataHubCompositeValue(contentType, contentLanguage, expected, 0L);
        LinkedDataHubCompositeValue linkedValue = new LinkedDataHubCompositeValue(value, Optional.<DataHubKey>absent(),
                Optional.<DataHubKey>absent());

        when(channelService.getValue(channelName, key.keyToString())).thenReturn(Optional.of(linkedValue));

        ChannelContentResource testClass = new ChannelContentResource(null, channelService);
        Response result = testClass.getValue(channelName, key.keyToString(), null);

        assertEquals(MediaType.TEXT_PLAIN_TYPE, result.getMetadata().getFirst("Content-Type"));
        assertEquals("en", result.getMetadata().getFirst("Content-Language"));
        assertEquals(expected, result.getEntity());
    }

    @Test
    public void testGetValueNoContentTypeSpecified() throws Exception {
        String channelName = "canal4";
        DataHubKey key = new SequenceDataHubKey( 1000);
        byte[] expected = new byte[]{55, 66, 77, 88};
        DataHubCompositeValue value = new DataHubCompositeValue(Optional.<String>absent(), Optional.<String>absent(), expected, 0L);
        Optional<DataHubKey> previous = Optional.absent();

        when(channelService.getValue(channelName, key.keyToString())).thenReturn(Optional.of(new LinkedDataHubCompositeValue(value, previous, Optional.<DataHubKey>absent())));

        ChannelContentResource testClass = new ChannelContentResource(null, channelService);
        Response result = testClass.getValue(channelName, key.keyToString(), null);

        assertEquals(MediaType.APPLICATION_OCTET_STREAM_TYPE, result.getMetadata().getFirst("Content-Type"));      //null, and the framework defaults to application/octet-stream
        assertEquals(expected, result.getEntity());
    }

    @Test
    public void testGetValueContentMismatch() throws Exception {
        String channelName = "canal4";
        DataHubKey key = new SequenceDataHubKey( 1000);
        byte[] expected = new byte[]{55, 66, 77, 88};
        DataHubCompositeValue value = new DataHubCompositeValue(Optional.of(MediaType.APPLICATION_XML), Optional.<String>absent(), expected, 0L);
        Optional<DataHubKey> previous = Optional.absent();

        when(channelService.getValue(channelName, key.keyToString())).thenReturn(Optional.of(new LinkedDataHubCompositeValue(value, previous, Optional.<DataHubKey>absent())));

        ChannelContentResource testClass = new ChannelContentResource(null, channelService);
        Response result = testClass.getValue(channelName, key.keyToString(), MediaType.APPLICATION_JSON);

        assertEquals(406, result.getStatus());
    }

    @Test
    public void testGetValueNotFound() throws Exception {

        String channelName = "canal4";
        DataHubKey key = new SequenceDataHubKey( 1000);

        when(channelService.getValue(channelName, key.keyToString())).thenReturn(Optional.<LinkedDataHubCompositeValue>absent());

        ChannelContentResource testClass = new ChannelContentResource(null, channelService);
        try {
            testClass.getValue(channelName, key.keyToString(), null);
            fail("Should have thrown exception.");
        } catch (WebApplicationException e) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), e.getResponse().getStatus());
        }
    }

    @Test
    public void testCreationDateHeaderInResponse() throws Exception {
        String channelName = "woo";
        DataHubKey key = new SequenceDataHubKey(  1000);
        DataHubCompositeValue value = new DataHubCompositeValue(Optional.<String>absent(), Optional.<String>absent(), "found it!".getBytes(), 987654321);
        LinkedDataHubCompositeValue linkedValue = new LinkedDataHubCompositeValue(value, Optional.<DataHubKey>absent(),
                Optional.<DataHubKey>absent());

        when(channelService.getValue(channelName, key.keyToString())).thenReturn(Optional.of(linkedValue));

        ChannelContentResource testClass = new ChannelContentResource(null, channelService);
        Response result = testClass.getValue(channelName, key.keyToString(), null);

        String creationDateString = (String) result.getMetadata().getFirst(CustomHttpHeaders.CREATION_DATE_HEADER.getHeaderName());
        assertEquals("1970-01-12T10:20:54.321Z", creationDateString);
    }

    @Test
    public void testPreviousLink() throws Exception {
        String channelName = "woo";
        DataHubKey previousKey = new SequenceDataHubKey(1000);
        DataHubKey key = new SequenceDataHubKey(1001);
        DataHubCompositeValue value = new DataHubCompositeValue(Optional.<String>absent(), Optional.<String>absent(), "found it!".getBytes(), 0L);
        Optional<DataHubKey> previous = Optional.of(previousKey);
        LinkedDataHubCompositeValue linkedValue = new LinkedDataHubCompositeValue(value, previous, Optional.<DataHubKey>absent());

        UriInfo uriInfo = mock(UriInfo.class);

        when(channelService.getValue(channelName, key.keyToString())).thenReturn(Optional.of(linkedValue));
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://path/to/thisitem123"));

        ChannelContentResource testClass = new ChannelContentResource(uriInfo, channelService);
        Response result = testClass.getValue(channelName, key.keyToString(), null);

        String link = (String) result.getMetadata().getFirst("Link");
        assertEquals("<http://path/to/1000>;rel=\"previous\"", link);
    }

    @Test
    public void testPreviousLink_none() throws Exception {
        String channelName = "woo";
        DataHubKey key = new SequenceDataHubKey( 1000);
        DataHubCompositeValue value = new DataHubCompositeValue(Optional.<String>absent(), Optional.<String>absent(), "found it!".getBytes(), 0L);
        Optional<DataHubKey> previous = Optional.absent();
        LinkedDataHubCompositeValue linkedValue = new LinkedDataHubCompositeValue(value, previous, Optional.<DataHubKey>absent());

        when(channelService.getValue(channelName, key.keyToString())).thenReturn(Optional.of(linkedValue));

        ChannelContentResource testClass = new ChannelContentResource(null, channelService);
        Response result = testClass.getValue(channelName, key.keyToString(), null);

        String link = (String) result.getMetadata().getFirst("Link");
        assertNull(link);
    }

    @Test
    public void testNextLink() throws Exception {
        String channelName = "nerxt";
        DataHubKey nextKey = new SequenceDataHubKey( 1001);
        DataHubKey key = new SequenceDataHubKey( 1000);
        DataHubCompositeValue value = new DataHubCompositeValue(Optional.<String>absent(), Optional.<String>absent(), "found it!".getBytes(), 0L);
        Optional<DataHubKey> next = Optional.of(nextKey);
        LinkedDataHubCompositeValue linkedValue = new LinkedDataHubCompositeValue(value, Optional.<DataHubKey>absent(), next);

        UriInfo uriInfo = mock(UriInfo.class);

        when(channelService.getValue(channelName, key.keyToString())).thenReturn(Optional.of(linkedValue));
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://path/to/thisitem123"));

        ChannelContentResource testClass = new ChannelContentResource(uriInfo, channelService);
        Response result = testClass.getValue(channelName, key.keyToString(), null);

        String link = (String) result.getMetadata().getFirst("Link");
        assertEquals("<http://path/to/1001>;rel=\"next\"", link);
    }

    @Test
    public void testNextLinkNone() throws Exception {
        String channelName = "nerxt";
        DataHubKey key = new SequenceDataHubKey( 1000);
        DataHubCompositeValue value = new DataHubCompositeValue(Optional.<String>absent(), Optional.<String>absent(), "found it!".getBytes(), 0L);
        LinkedDataHubCompositeValue linkedValue = new LinkedDataHubCompositeValue(value, Optional.<DataHubKey>absent(),
                Optional.<DataHubKey>absent());


        when(channelService.getValue(channelName, key.keyToString())).thenReturn(Optional.of(linkedValue));

        ChannelContentResource testClass = new ChannelContentResource(null, channelService);
        Response result = testClass.getValue(channelName, key.keyToString(), null);

        String link = (String) result.getMetadata().getFirst("Link");
        assertNull(link);
    }

    @Test
    public void testLanguageHeader_missing() throws Exception {
        String channelName = "canal4";
        DataHubKey key = new SequenceDataHubKey( 1000);
        DataHubCompositeValue value = new DataHubCompositeValue(Optional.<String>absent(), Optional.<String>absent(), new byte[]{}, 0L);
        LinkedDataHubCompositeValue linkedValue = new LinkedDataHubCompositeValue(value, Optional.<DataHubKey>absent(),
                Optional.<DataHubKey>absent());

        when(channelService.getValue(channelName, key.keyToString())).thenReturn(Optional.of(linkedValue));

        ChannelContentResource testClass = new ChannelContentResource(null, channelService);
        Response result = testClass.getValue(channelName, key.keyToString(), null);

        assertNull(result.getMetadata().getFirst("Content-Language"));
    }

    @Test
    public void testEncodingHeader_missing() throws Exception {
        String channelName = "canal4";
        DataHubKey key = new SequenceDataHubKey( 1000);
        DataHubCompositeValue value = new DataHubCompositeValue(Optional.<String>absent(), Optional.<String>absent(), new byte[]{}, 0L);
        LinkedDataHubCompositeValue linkedValue = new LinkedDataHubCompositeValue(value, Optional.<DataHubKey>absent(),
                Optional.<DataHubKey>absent());

        when(channelService.getValue(channelName, key.keyToString())).thenReturn(Optional.of(linkedValue));

        ChannelContentResource testClass = new ChannelContentResource(null, channelService);
        Response result = testClass.getValue(channelName, key.keyToString(), null);

        assertNull(result.getMetadata().getFirst("Content-Encoding"));
    }
}
