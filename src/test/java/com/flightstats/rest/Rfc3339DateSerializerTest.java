package com.flightstats.rest;

import com.fasterxml.jackson.core.JsonGenerator;
import com.flightstats.hub.rest.Rfc3339DateSerializer;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class Rfc3339DateSerializerTest {

    @Test
    void testSerialize() throws Exception {
        Rfc3339DateSerializer testClass = new Rfc3339DateSerializer();
        Date date = new Date(493959282725L);
        JsonGenerator jgen = mock(JsonGenerator.class);
        testClass.serialize(date, jgen, null);
        verify(jgen).writeString("1985-08-27T02:54:42.725Z");
    }
}
