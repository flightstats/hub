package com.flightstats.hub.model;

import com.flightstats.hub.model.adapters.HubDateTimeTypeAdapter;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HubDateTimeTypeAdapterTest {

    @Test
    void testDeserialize() throws Exception {
        doTest("2016-11-09T21:23:20.123Z", "2016-11-09T21:23:20.123Z");
    }

    @Test
    void testDeserializeNoMillis() throws Exception {
        doTest("2016-11-09T21:23:20Z", "2016-11-09T21:23:20.000Z");
    }

    @Test
    void testNoTimeZone() throws Exception {
        doTest("2016-11-09T13:16:35.865", "2016-11-09T13:16:35.865Z");
    }

    @Test
    void testDateOnly() throws Exception {
        doTest("2016-11-09", "2016-11-09T00:00:00.000Z");
    }

    @Test
    void testDateHour() throws Exception {
        doTest("2016-11-09T12", "2016-11-09T12:00:00.000Z");
    }

    @Test
    void testDateHourMinute() throws Exception {
        doTest("2016-11-09T12:10", "2016-11-09T12:10:00.000Z");
    }

    private void doTest(String input, String expected) {
        DateTime parsed = HubDateTimeTypeAdapter.deserialize(input);
        System.out.println("parse " + parsed);
        assertEquals(expected, ISODateTimeFormat.dateTime().print(parsed));
    }


}