package com.flightstats.rest;

import com.flightstats.hub.rest.HalLink;
import com.flightstats.hub.rest.Linked;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LinkedTest {

    @Test
    public void testBuilder() throws Exception {
        List<HalLink> expectedLinks = Arrays.asList(new HalLink("foo", URI.create("http://lycos.com")),
                new HalLink("bar", URI.create("http://yahoo.com")),
                new HalLink("foo", URI.create("http://hotmail.com")));

        Linked<String> buildResult = Linked.linked("hello")
                .withLink("foo", "http://lycos.com")
                .withLink("bar", "http://yahoo.com")
                .withLink("foo", "http://hotmail.com")
                .build();

        List<HalLink> linksList = buildResult.getHalLinks().getLinks();
        assertEquals(linksList, expectedLinks);
    }
}
