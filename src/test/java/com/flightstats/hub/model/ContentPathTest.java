package com.flightstats.hub.model;

import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentPathTest {

    @Test
    void testContentKeyFromFullUrl() {
        Optional<ContentPath> optional = ContentPath.fromFullUrl("http://hub/channel/load_test_2/2015/01/23/21/11/19/407/L7QtaY");
        assertTrue(optional.isPresent());
        ContentPath path = optional.get();
        assertEquals("2015/01/23/21/11/19/407/L7QtaY", path.toUrl());
        assertTrue(path instanceof ContentKey);
    }

    @Test
    void testMinutePathFromFullUrl() {
        Optional<ContentPath> optional = ContentPath.fromFullUrl("http://hub/channel/load_test_2/2015/01/23/21/11");
        assertTrue(optional.isPresent());
        ContentPath path = optional.get();
        assertEquals("2015/01/23/21/11", path.toUrl());
        assertTrue(path instanceof MinutePath);
    }

    @Test
    void testContentKeyFromUrl() {
        Optional<ContentPath> optional = ContentPath.fromUrl("2015/01/23/21/11/19/407/L7QtaY");
        assertTrue(optional.isPresent());
        ContentPath path = optional.get();
        assertEquals("2015/01/23/21/11/19/407/L7QtaY", path.toUrl());
        assertTrue(path instanceof ContentKey);
    }

    @Test
    void testMinutePathFromUrl() {
        Optional<ContentPath> optional = ContentPath.fromUrl("2015/01/23/21/11");
        assertTrue(optional.isPresent());
        ContentPath path = optional.get();
        assertEquals("2015/01/23/21/11", path.toUrl());
        assertTrue(path instanceof MinutePath);
    }

}