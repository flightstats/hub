package com.flightstats.hub.spoke;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SpokeManagerTest {

    @Test
    public void testQuorum() {

        assertEquals(1, SpokeManager.getQuorum(1));
        assertEquals(1, SpokeManager.getQuorum(2));
        assertEquals(2, SpokeManager.getQuorum(3));
        assertEquals(2, SpokeManager.getQuorum(4));
        assertEquals(3, SpokeManager.getQuorum(5));
        assertEquals(1, SpokeManager.getQuorum(1));

    }

}