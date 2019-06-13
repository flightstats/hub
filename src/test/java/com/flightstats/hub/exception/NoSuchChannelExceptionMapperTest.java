package com.flightstats.hub.exception;

import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoSuchChannelExceptionMapperTest {

    @Test
    void testMap() {
        NoSuchChannelExceptionMapper testClass = new NoSuchChannelExceptionMapper();
        NoSuchChannelException exception = new NoSuchChannelException("No such channel: flimflam", new RuntimeException("boom"));

        Response result = testClass.toResponse(exception);

        assertEquals("No such channel: flimflam", result.getEntity());
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), result.getStatus());
    }
}