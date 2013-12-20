package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.SequenceDataHubKey;
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

        assertEquals("blah:1", strategy.buildKey("blah", new SequenceDataHubKey(1000)));
        assertEquals("blah:1", strategy.buildKey("blah", new SequenceDataHubKey(1001)));
        assertEquals("blah:1", strategy.buildKey("blah", new SequenceDataHubKey(1999)));
        assertEquals("blah:2", strategy.buildKey("blah", new SequenceDataHubKey(2000)));
        assertEquals("blah:2", strategy.buildKey("blah", new SequenceDataHubKey(2001)));
        assertEquals("toot:15", strategy.buildKey("toot", new SequenceDataHubKey(15642)));
    }

}
