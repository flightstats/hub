package com.flightstats.hub.model;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ContentPathTest {

    @Test
    public void testContentKeyFromFullUrl() {
        Optional<ContentPath> optional = ContentPath.fromFullUrl("http://hub/channel/load_test_2/2015/01/23/21/11/19/407/L7QtaY");
        assertTrue(optional.isPresent());
        ContentPath path = optional.get();
        assertEquals("2015/01/23/21/11/19/407/L7QtaY", path.toUrl());
        assertTrue(path instanceof ContentKey);
    }

    @Test
    public void testMinutePathFromFullUrl() {
        Optional<ContentPath> optional = ContentPath.fromFullUrl("http://hub/channel/load_test_2/2015/01/23/21/11");
        assertTrue(optional.isPresent());
        ContentPath path = optional.get();
        assertEquals("2015/01/23/21/11", path.toUrl());
        assertTrue(path instanceof MinutePath);
    }

    @Test
    public void testContentKeyFromUrl() {
        Optional<ContentPath> optional = ContentPath.fromUrl("2015/01/23/21/11/19/407/L7QtaY");
        assertTrue(optional.isPresent());
        ContentPath path = optional.get();
        assertEquals("2015/01/23/21/11/19/407/L7QtaY", path.toUrl());
        assertTrue(path instanceof ContentKey);
    }

    @Test
    public void testMinutePathFromUrl() {
        Optional<ContentPath> optional = ContentPath.fromUrl("2015/01/23/21/11");
        assertTrue(optional.isPresent());
        ContentPath path = optional.get();
        assertEquals("2015/01/23/21/11", path.toUrl());
        assertTrue(path instanceof MinutePath);
    }

}