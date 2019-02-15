package com.flightstats.hub.dao;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class ItemRequestTest {

    @Test
    public void testCached() {
        ItemRequest request = ItemRequest.builder().channel("name").build();
        assertFalse(request.isRemoteOnly());

    }
}