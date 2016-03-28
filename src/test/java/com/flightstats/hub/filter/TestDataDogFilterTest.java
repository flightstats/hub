package com.flightstats.hub.filter;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class TestDataDogFilterTest {

    @Test
    public void testChannelTemplateLatestPaths() {
        DataDogRequestFilter filter = new DataDogRequestFilter();
        String method = "GET";
        List<String[]> testsExpects = new ArrayList<>();
        testsExpects.add(new String[]{"channel/test/latest", "channel/channel/latest"});
        testsExpects.add(new String[]{"channel/test/2016/latest", "channel/channel/year/latest"});
        testsExpects.add(new String[]{"channel/test/2016/03/latest", "channel/channel/year/month/latest"});
        testsExpects.add(new String[]{"channel/test/2016/03/24/latest", "channel/channel/year/month/day/latest"});
        testsExpects.add(new String[]{"channel/test/2016/03/24/14/latest", "channel/channel/year/month/day/hour/latest"});
        testsExpects.add(new String[]{"channel/test/2016/03/24/14/31/latest", "channel/channel/year/month/day/hour/minute/latest"});
        testsExpects.add(new String[]{"channel/test/2016/03/24/14/31/25/latest", "channel/channel/year/month/day/hour/minute/second/latest"});

        for (String[] test : testsExpects) {
            assertTrue(filter.constructDeclaredpath(test[0], method).equals(test[1]));
        }
    }

    @Test
    public void testChanelWildcards() {
        DataDogRequestFilter filter = new DataDogRequestFilter();
        String method = "GET";
        List<String[]> testsExpects = new ArrayList<>();
        testsExpects.add(new String[]{"channel/test", "channel/channel"});
        testsExpects.add(new String[]{"channel/test/2016", "channel/channel/year"});
        testsExpects.add(new String[]{"channel/test/2016/03", "channel/channel/year/month"});
        testsExpects.add(new String[]{"channel/test/2016/03/24", "channel/channel/year/month/day"});
        testsExpects.add(new String[]{"channel/test/2016/03/24/14", "channel/channel/year/month/day/hour"});
        testsExpects.add(new String[]{"channel/test/2016/03/24/14/31", "channel/channel/year/month/day/hour/minute"});
        testsExpects.add(new String[]{"channel/test/2016/03/24/14/31/25", "channel/channel/year/month/day/hour/minute/second"});
    }
}
