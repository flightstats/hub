package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.DataHubKey;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class ChannelHourRowKeyStrategyTest {

    @Test
    public void testBuildKey() throws Exception {
    	//GIVEN
        ChannelHourRowKeyStrategy testClass = new ChannelHourRowKeyStrategy();

        //WHEN
        DataHubKey key = new DataHubKey(new Date(9021099999L), (short)9);
        String result = testClass.buildKey("fooChan", key);

        //THEN
    	assertEquals("fooChan:1970041509", result);
    }

    @Test
    public void testNextKey() throws Exception {
        ChannelHourRowKeyStrategy testClass = new ChannelHourRowKeyStrategy();

        //WHEN
        String result = testClass.nextKey("fooChan", "fooChan:1970041501");

    	//THEN
        assertEquals("fooChan:1970041502", result);
    }

    @Test
    public void testPrevKey() throws Exception {
        ChannelHourRowKeyStrategy testClass = new ChannelHourRowKeyStrategy();

        //WHEN
        String result = testClass.prevKey("fooChan", "fooChan:1970041502");

        //THEN
        assertEquals("fooChan:1970041501", result);
    }
}
