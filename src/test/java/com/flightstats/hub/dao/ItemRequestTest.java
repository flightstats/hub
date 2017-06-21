package com.flightstats.hub.dao;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class ItemRequestTest {

    @Test
    public void testCached() {
        ItemRequest request = ItemRequest.builder().channel("name").build();
        assertFalse(request.isRemoteOnly());

    }
}