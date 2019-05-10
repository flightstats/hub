package com.flightstats.hub.spoke;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SpokePathUtilTest {
    @Test
    void testYear() throws Exception {
        assertEquals("2014", SpokePathUtil.year("alksdjf/2014"));
        assertNotEquals("2014", SpokePathUtil.year("alksdjf2014"));
        assertEquals("10", SpokePathUtil.month("alksdjf/2014/10"));
        assertEquals("05", SpokePathUtil.day("alksdjf/2014/10/05"));
        assertEquals("13", SpokePathUtil.hour("alksdjf/2014/10/05/13"));
        assertEquals("09", SpokePathUtil.minute("alksdjf/2014/10/05/13/09"));
        assertEquals("15", SpokePathUtil.second("alksdjf/2014/10/05/13/09/15123aaa"));
        assertNull(SpokePathUtil.minute("alksdjf/2014/10/05/13/0"));
        assertEquals("second", SpokePathUtil.smallestTimeResolution("lkjsldfjlkasd/2014/12/12/12/12/12"));
        assertEquals("year", SpokePathUtil.smallestTimeResolution("lkjsldfjlkasd/2014/"));
        assertEquals("alksdjf/2014/10/05/13/09/15", SpokePathUtil.secondPathPart("alksdjf/2014/10/05/13/09/15123aaa"));
    }
}
