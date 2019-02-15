package com.flightstats.hub.model;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HubDateTimeTypeAdapterTest {

    @Test
    public void testDeserialize() throws Exception {
        doTest("2016-11-09T21:23:20.123Z", "2016-11-09T21:23:20.123Z");
    }

    @Test
    public void testDeserializeNoMillis() throws Exception {
        doTest("2016-11-09T21:23:20Z", "2016-11-09T21:23:20.000Z");
    }

    @Test
    public void testNoTimeZone() throws Exception {
        doTest("2016-11-09T13:16:35.865", "2016-11-09T13:16:35.865Z");
    }

    @Test
    public void testDateOnly() throws Exception {
        doTest("2016-11-09", "2016-11-09T00:00:00.000Z");
    }

    @Test
    public void testDateHour() throws Exception {
        doTest("2016-11-09T12", "2016-11-09T12:00:00.000Z");
    }

    @Test
    public void testDateHourMinute() throws Exception {
        doTest("2016-11-09T12:10", "2016-11-09T12:10:00.000Z");
    }

    private void doTest(String input, String expected) {
        DateTime parsed = HubDateTimeTypeAdapter.deserialize(input);
        System.out.println("parse " + parsed);
        assertEquals(expected, ISODateTimeFormat.dateTime().print(parsed));
    }


}