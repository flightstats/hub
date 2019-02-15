package com.flightstats.rest;

import com.fasterxml.jackson.core.JsonGenerator;
import com.flightstats.hub.rest.HalLink;
import com.flightstats.hub.rest.HalLinks;
import com.flightstats.hub.rest.HalLinksSerializer;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.*;

public class HalLinksSerializerTest {

    @Test
    public void testSerialize() throws Exception {
        String fooUri = "/path/to/foo";
        String barUri = "http://bar.com";
        String channel1Uri = "http://hub/channel/ch1";
        String channel2Uri = "http://hub/channel/ch2";

        HalLink link1 = new HalLink("foo", URI.create(fooUri));
        HalLink link2 = new HalLink("bar", URI.create(barUri));
        Multimap<String, HalLink> multiLinks = ArrayListMultimap.create();
        multiLinks.put("channels", new HalLink("chan1", URI.create(channel1Uri)));
        multiLinks.put("channels", new HalLink("chan2", URI.create(channel2Uri)));
        HalLinks halLinks = new HalLinks(Arrays.asList(link1, link2), multiLinks);

        JsonGenerator jgen = mock(JsonGenerator.class);

        HalLinksSerializer testClass = new HalLinksSerializer();

        testClass.serialize(halLinks, jgen, null);
        InOrder inOrder = inOrder(jgen);
        inOrder.verify(jgen).writeStartObject();

        inOrder.verify(jgen).writeFieldName("foo");
        inOrder.verify(jgen).writeStartObject();
        inOrder.verify(jgen).writeStringField("href", fooUri);
        inOrder.verify(jgen).writeEndObject();

        inOrder.verify(jgen).writeFieldName("bar");
        inOrder.verify(jgen).writeStartObject();
        inOrder.verify(jgen).writeStringField("href", barUri);
        inOrder.verify(jgen).writeEndObject();

        inOrder.verify(jgen).writeFieldName("channels");
        inOrder.verify(jgen).writeStartArray();
        inOrder.verify(jgen).writeStartObject();
        inOrder.verify(jgen).writeStringField("name", "chan1");
        inOrder.verify(jgen).writeStringField("href", channel1Uri);
        inOrder.verify(jgen).writeEndObject();

        inOrder.verify(jgen).writeStartObject();
        inOrder.verify(jgen).writeStringField("name", "chan2");
        inOrder.verify(jgen).writeStringField("href", channel2Uri);
        inOrder.verify(jgen).writeEndObject();
        inOrder.verify(jgen).writeEndArray();

        inOrder.verify(jgen).writeEndObject();

        verifyNoMoreInteractions(jgen);
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
        verifyNoMoreInteractions(jgen);
    }
}
