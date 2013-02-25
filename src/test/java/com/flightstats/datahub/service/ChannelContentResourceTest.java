package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChannelContentResourceTest {

    @Test
    public void testGetValue() throws Exception {

        String channelName = "canal4";
        byte[] expected = new byte[]{55, 66, 77, 88};
        String contentType = "text/plain";
        DataHubKey key = new DataHubKey(new Date(11), (short) 0);
        DataHubKeyRenderer dataHubKeyRenderer = new DataHubKeyRenderer();

        ChannelDao dao = mock(ChannelDao.class);

        when(dao.getValue(channelName, key)).thenReturn(new DataHubCompositeValue(contentType, expected));

        ChannelContentResource testClass = new ChannelContentResource(dao, dataHubKeyRenderer);
        Response result = testClass.getValue(channelName, dataHubKeyRenderer.keyToString(key));

        assertEquals(MediaType.TEXT_PLAIN_TYPE, result.getMetadata().getFirst("Content-Type"));
        assertEquals(expected, result.getEntity());
    }

    @Test
    public void testGetValueNoContentTypeSpecified() throws Exception {
        String channelName = "canal4";
        DataHubKey key = new DataHubKey(new Date(11), (short) 0);
        DataHubKeyRenderer dataHubKeyRenderer = new DataHubKeyRenderer();

        byte[] expected = new byte[]{55, 66, 77, 88};

        ChannelDao dao = mock(ChannelDao.class);

        when(dao.getValue(channelName, key)).thenReturn(new DataHubCompositeValue(null, expected));

        ChannelContentResource testClass = new ChannelContentResource(dao, dataHubKeyRenderer);
        Response result = testClass.getValue(channelName, dataHubKeyRenderer.keyToString(key));

        assertNull(result.getMetadata().getFirst("Content-Type"));      //null, and the framework defaults to application/octet-stream
        assertEquals(expected, result.getEntity());
    }

    @Test
    public void testGetValueNotFound() throws Exception {

        String channelName = "canal4";
        DataHubKey key = new DataHubKey(new Date(11), (short) 0);
        DataHubKeyRenderer dataHubKeyRenderer = new DataHubKeyRenderer();

        ChannelDao dao = mock(ChannelDao.class);

        when(dao.getValue(channelName, key)).thenReturn(null);

        ChannelContentResource testClass = new ChannelContentResource(dao, dataHubKeyRenderer);
        try {
            testClass.getValue(channelName, dataHubKeyRenderer.keyToString(key));
            fail("Should have thrown exception.");
        } catch (WebApplicationException e) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), e.getResponse().getStatus());
        }
    }

    @Test
    public void testCreationDateHeaderInResponse() throws Exception {
        String channelName = "woo";
        DataHubKey key = new DataHubKey(new Date(1123456678922L), (short) 0);
        DataHubKeyRenderer dataHubKeyRenderer = new DataHubKeyRenderer();

        ChannelDao dao = mock(ChannelDao.class);

        when(dao.getValue(channelName, key)).thenReturn(new DataHubCompositeValue(null, "found it!".getBytes()));

        ChannelContentResource testClass = new ChannelContentResource(dao, dataHubKeyRenderer);
        Response result = testClass.getValue(channelName, dataHubKeyRenderer.keyToString(key));

        String creationDateString = (String) result.getMetadata().getFirst(CustomHttpHeaders.CREATION_DATE_HEADER.getHeaderName());
        assertEquals("2005-08-07T23:17:58.922Z", creationDateString);
    }
}
