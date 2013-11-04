package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.DataHubKey;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 *
 */
public class SequenceRowKeyStrategyTest {

    private SequenceRowKeyStrategy strategy;

    @Before
    public void setUp() throws Exception {
        strategy = new SequenceRowKeyStrategy();
    }

    @Test
    public void testBuildKey() throws Exception {
        assertEquals("blah:0", strategy.buildKey("blah", new DataHubKey(999)));
        assertEquals("blah:1", strategy.buildKey("blah", new DataHubKey(1000)));
        assertEquals("blah:1", strategy.buildKey("blah", new DataHubKey(1001)));
        assertEquals("blah:1", strategy.buildKey("blah", new DataHubKey(1999)));
        assertEquals("blah:2", strategy.buildKey("blah", new DataHubKey(2000)));
        assertEquals("blah:2", strategy.buildKey("blah", new DataHubKey(2001)));
        assertEquals("toot:15", strategy.buildKey("toot", new DataHubKey(15642)));
    }

    @Test
    public void testNextKey() throws Exception {
        assertEquals("bleh:11", strategy.nextKey("bleh", "bleh:10"));
        assertEquals("bleh:100", strategy.nextKey("bleh", "bleh:99"));
    }

    @Test
    public void testPrevKey() throws Exception {
        assertEquals("bleh:9", strategy.prevKey("bleh", "bleh:10"));
        assertEquals("bleh:98", strategy.prevKey("bleh", "bleh:99"));
        assertEquals("bleh:99", strategy.prevKey("bleh", "bleh:100"));
    }
}
