package com.flightstats.hub.filter;

import com.flightstats.hub.app.HubProperties;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestDataDogFilterTest {

    @BeforeClass
    public static void setupClass() {
        HubProperties.setProperty(DataDogRequestFilter.HUB_DATADOG_METRICS_FLAG, "true");
    }

    @Test
    public void testChannelTemplateLatestPaths() {
        DataDogRequestFilter filter = new DataDogRequestFilter();
        String method = "GET";
        List<String[]> testsExpects = new ArrayList<>();
        testsExpects.add(new String[]{"channel/test/latest", "/channel/channel/latest"});
        testsExpects.add(new String[]{"channel/test/2016/latest", "/channel/channel/Y/latest"});
        testsExpects.add(new String[]{"channel/test/2016/03/latest", "/channel/channel/Y/M/latest"});
        testsExpects.add(new String[]{"channel/test/2016/03/24/latest", "/channel/channel/Y/M/D/latest"});
        testsExpects.add(new String[]{"channel/test/2016/03/24/14/latest", "/channel/channel/Y/M/D/h/latest"});
        testsExpects.add(new String[]{"channel/test/2016/03/24/14/31/latest", "/channel/channel/Y/M/D/h/m/latest"});
        testsExpects.add(new String[]{"channel/test/2016/03/24/14/31/25/latest", "/channel/channel/Y/M/D/h/m/s/latest"});

        for (String[] test : testsExpects) {
            assertEquals(filter.constructDeclaredpath(test[0], method), test[1]);
        }
    }

    @Test
    public void testChanelWildcards() {
        DataDogRequestFilter filter = new DataDogRequestFilter();
        String method = "GET";
        List<String[]> testsExpects = new ArrayList<>();
        testsExpects.add(new String[]{"channel/test", "/channel/channel"});
        testsExpects.add(new String[]{"channel/test/2016", "/channel/channel/Y"});
        testsExpects.add(new String[]{"channel/test/2016/03", "/channel/channel/Y/M"});
        testsExpects.add(new String[]{"channel/test/2016/03/24", "/channel/channel/Y/M/D"});
        testsExpects.add(new String[]{"channel/test/2016/03/24/14", "/channel/channel/Y/M/D/h"});
        testsExpects.add(new String[]{"channel/test/2016/03/24/14/31", "/channel/channel/Y/M/D/h/m"});
        testsExpects.add(new String[]{"channel/test/2016/03/24/14/31/25", "/channel/channel/Y/M/D/h/m/s"});

        for (String[] test : testsExpects) {
            assertEquals(filter.constructDeclaredpath(test[0], method), test[1]);
        }
    }

    @Test
    public void testChannelTime() {
        DataDogRequestFilter filter = new DataDogRequestFilter();
        String method = "GET";
        List<String[]> testsExpects = new ArrayList<>();
        testsExpects.add(new String[]{"channel/test/time", "/channel/channel/time"});
        testsExpects.add(new String[]{"channel/test/time/hour", "/channel/channel/time/hour"});
        testsExpects.add(new String[]{"channel/test/time/minute", "/channel/channel/time/minute"});
        testsExpects.add(new String[]{"channel/test/time/second", "/channel/channel/time/second"});
        testsExpects.add(new String[]{"channel/test/time/day", "/channel/channel/time/day"});

        for (String[] test : testsExpects) {
            assertEquals(filter.constructDeclaredpath(test[0], method), test[1]);
        }
    }

    @Test
    public void testChannelEarliest() {
        DataDogRequestFilter filter = new DataDogRequestFilter();
        String method = "GET";
        List<String[]> testsExpects = new ArrayList<>();
        testsExpects.add(new String[]{"channel/test/earliest", "/channel/channel/earliest"});

        for (String[] test : testsExpects) {
            assertEquals(filter.constructDeclaredpath(test[0], method), test[1]);
        }
    }

    @Test
    public void testChannelBulk() {
        DataDogRequestFilter filter = new DataDogRequestFilter();
        String method = "GET";
        List<String[]> testsExpects = new ArrayList<>();
        testsExpects.add(new String[]{"channel/test/bulk", "/channel/channel/bulk"});

        for (String[] test : testsExpects) {
            assertEquals(filter.constructDeclaredpath(test[0], method), test[1]);
        }
    }

    @Test
    public void testChannelBatch() {
        DataDogRequestFilter filter = new DataDogRequestFilter();
        String method = "GET";
        List<String[]> testsExpects = new ArrayList<>();
        testsExpects.add(new String[]{"channel/test/batch", "/channel/channel/batch"});

        for (String[] test : testsExpects) {
            assertEquals(filter.constructDeclaredpath(test[0], method), test[1]);
        }
    }

    @Test
    public void testSpokePayloadPath() {
        DataDogRequestFilter filter = new DataDogRequestFilter();
        String method = "GET";
        List<String[]> testsExpects = new ArrayList<>();
        testsExpects.add(new String[]{"internal/spoke/payload/somechannel/somekey", "/internal/spoke/payload/path"});

        for (String[] test : testsExpects) {
            assertEquals(filter.constructDeclaredpath(test[0], method), test[1]);
        }
    }

    @Test
    public void testInternalSpokeTimeTimePath() {
        DataDogRequestFilter filter = new DataDogRequestFilter();
        String method = "GET";
        List<String[]> testsExpects = new ArrayList<>();
        testsExpects.add(new String[]{"internal/spoke/time/2016", "/internal/spoke/time/Y"});
        testsExpects.add(new String[]{"internal/spoke/time/2016/03", "/internal/spoke/time/Y/M"});
        testsExpects.add(new String[]{"internal/spoke/time/2016/03/21", "/internal/spoke/time/Y/M/D"});
        testsExpects.add(new String[]{"internal/spoke/time/2016/03/21/12", "/internal/spoke/time/Y/M/D/h"});
        testsExpects.add(new String[]{"internal/spoke/time/2016/03/21/12/31", "/internal/spoke/time/Y/M/D/h/m"});
        testsExpects.add(new String[]{"internal/spoke/time/2016/03/21/12/31/15", "/internal/spoke/time/Y/M/D/h/m/s"});
        testsExpects.add(new String[]{"internal/spoke/time/2016/03/21/12/31/15/500", "/internal/spoke/time/Y/M/D/h/m/s/ms"});

        for (String[] test : testsExpects) {
            assertEquals(filter.constructDeclaredpath(test[0], method), test[1]);
        }
    }

    @Test
    public void testTags() {
        DataDogRequestFilter filter = new DataDogRequestFilter();
        String method = "GET";
        List<String[]> testsExpects = new ArrayList<>();
        testsExpects.add(new String[]{"tag", "/tag"});
        testsExpects.add(new String[]{"tag/test", "/tag/tag"});
        testsExpects.add(new String[]{"tag/test/latest?stable=true", "/tag/tag/latest"});
        testsExpects.add(new String[]{"tag/test/2016/04/01/17/59/54/656/KXo3du/previous/10000", "/tag/tag/Y/M/D/h/m/s/ms/hash/previous/count"});
        testsExpects.add(new String[]{"tag/test/2016/04/01/14/48/23/468/E9Yfkc/next/100", "/tag/tag/Y/M/D/h/m/s/ms/hash/next/count"});
        testsExpects.add(new String[]{"tag/test/2016/03/30/12/43/33/430/y8t02V", "/tag/tag/Y/M/D/h/m/s/ms/hash"});

        for (String[] test : testsExpects) {
            assertEquals(filter.constructDeclaredpath(test[0], method), test[1]);
        }
    }


}
