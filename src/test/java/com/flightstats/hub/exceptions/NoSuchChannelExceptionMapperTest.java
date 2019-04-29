package com.flightstats.hub.exceptions;

import com.flightstats.hub.exception.NoSuchChannelException;
import com.flightstats.hub.exception.NoSuchChannelExceptionMapper;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoSuchChannelExceptionMapperTest {

    @Test
    void testMap() throws Exception {
        //GIVEN
        NoSuchChannelExceptionMapper testClass = new NoSuchChannelExceptionMapper();
        NoSuchChannelException exception = new NoSuchChannelException("No such channel: flimflam", new RuntimeException("boom"));
        //WHEN
        Response result = testClass.toResponse(exception);
        //THEN
        assertEquals("No such channel: flimflam", result.getEntity());
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), result.getStatus());
    }
}
