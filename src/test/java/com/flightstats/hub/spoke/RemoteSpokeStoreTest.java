package com.flightstats.hub.spoke;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RemoteSpokeStoreTest {

    @Test
    public void testQuorum() {

        assertEquals(1, RemoteClusterSpokeStore.getQuorum(1));
        assertEquals(1, RemoteClusterSpokeStore.getQuorum(2));
        assertEquals(2, RemoteClusterSpokeStore.getQuorum(3));
        assertEquals(2, RemoteClusterSpokeStore.getQuorum(4));
        assertEquals(3, RemoteClusterSpokeStore.getQuorum(5));
        assertEquals(1, RemoteClusterSpokeStore.getQuorum(1));

    }

}