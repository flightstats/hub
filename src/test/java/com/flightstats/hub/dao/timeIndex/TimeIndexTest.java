package com.flightstats.hub.dao.timeIndex;

import org.joda.time.DateTime;

/**
 *
 */
public class TimeIndexTest {

    private DateTime dateTime;

    //todo - gfm - 10/28/14 - go away?
    /*@Before
    public void setUp() throws Exception {
        dateTime = new DateTime(2013, 12, 26, 12, 59);
    }

    @Test
    public void testHashstamp() throws Exception {
        String hashStamp = TimeIndex.getHash(dateTime);
        assertEquals("2013-12-26T20:59+0000", hashStamp);
    }

    @Test
    public void testParse() throws Exception {
        DateTime parsed = TimeIndex.parseHash("2013-12-26T12:59-0800");
        assertEquals(dateTime, parsed);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoMinutes() throws Exception {
        TimeIndex.parseHash("2013-12-26T12");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongFormat() throws Exception {
        TimeIndex.parseHash("2013/12-26T12:59");
    }

    @Test
    public void testDaylightSavingsTime() throws Exception {
        assertEquals("2014-11-02T07:30+0000", TimeIndex.getHash(TimeIndex.parseHash("2014-11-02T00:30-0700")));
        assertEquals("2014-11-02T08:30+0000", TimeIndex.getHash(TimeIndex.parseHash("2014-11-02T01:30-0700")));
        assertEquals("2014-11-02T09:30+0000", TimeIndex.getHash(TimeIndex.parseHash("2014-11-02T02:30-0700")));
        assertEquals("2014-11-02T10:30+0000", TimeIndex.getHash(TimeIndex.parseHash("2014-11-02T02:30-0800")));
    }

    @Test
    public void testTImeZones() throws Exception {
        assertEquals("2014-10-02T12:30+0000", TimeIndex.getHash(TimeIndex.parseHash("2014-10-02T12:30-0000")));
    }

    @Test
    public void testPath() throws Exception {
        assertEquals("/TimeIndex", TimeIndex.getPath());
    }

    @Test
    public void testPathChannel() throws Exception {
        assertEquals("/TimeIndex/someChannel", TimeIndex.getPath("someChannel"));
    }

    @Test
    public void testPathChannelTime() throws Exception {
        assertEquals("/TimeIndex/someChannel/2013-12-26T12:59-0800", TimeIndex.getPath("someChannel", "2013-12-26T12:59-0800"));
    }

    @Test
    public void testPathFull() throws Exception {
        assertEquals("/TimeIndex/someChannel/2013-12-26T20:59+0000/999", TimeIndex.getPath("someChannel", dateTime, new ContentKey(999)));
    }*/


}
