package com.flightstats.hub.spoke;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RemoteSpokeStoreTest {

    @Test
    public void testQuorum() {

        assertEquals(1, RemoteSpokeStore.getQuorum(1));
        assertEquals(1, RemoteSpokeStore.getQuorum(2));
        assertEquals(2, RemoteSpokeStore.getQuorum(3));
        assertEquals(2, RemoteSpokeStore.getQuorum(4));
        assertEquals(3, RemoteSpokeStore.getQuorum(5));
        assertEquals(1, RemoteSpokeStore.getQuorum(1));

    }

}