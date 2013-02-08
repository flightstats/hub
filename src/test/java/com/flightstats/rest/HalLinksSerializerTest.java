package com.flightstats.rest;

import org.codehaus.jackson.JsonGenerator;
import org.junit.Test;
import org.mockito.InOrder;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.*;

public class HalLinksSerializerTest {

    @Test
    public void testSerialize() throws Exception {
        HalLink link1 = new HalLink("foo", URI.create("/path/to/foo"));
        HalLink link2 = new HalLink("bar", URI.create("http://bar.com"));
        HalLinks halLinks = new HalLinks(Arrays.asList(link1, link2));

        JsonGenerator jgen = mock(JsonGenerator.class);

        HalLinksSerializer testClass = new HalLinksSerializer();

        testClass.serialize(halLinks, jgen, null);
        InOrder inOrder = inOrder(jgen);
        inOrder.verify(jgen).writeStartObject();

        inOrder.verify(jgen).writeFieldName("foo");
        inOrder.verify(jgen).writeStartObject();
        inOrder.verify(jgen).writeFieldName("href");
        inOrder.verify(jgen).writeObject("/path/to/foo");
        inOrder.verify(jgen).writeEndObject();

        inOrder.verify(jgen).writeFieldName("bar");
        inOrder.verify(jgen).writeStartObject();
        inOrder.verify(jgen).writeFieldName("href");
        inOrder.verify(jgen).writeObject("http://bar.com");
        inOrder.verify(jgen, times(2)).writeEndObject();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSerializeNoLinks() throws Exception {
        HalLinks halLinks = new HalLinks(Collections.EMPTY_LIST);

        JsonGenerator jgen = mock(JsonGenerator.class);

        HalLinksSerializer testClass = new HalLinksSerializer();

        testClass.serialize(halLinks, jgen, null);
        InOrder inOrder = inOrder(jgen);
        inOrder.verify(jgen).writeStartObject();
        inOrder.verify(jgen).writeEndObject();
        inOrder.verifyNoMoreInteractions();
    }
}
