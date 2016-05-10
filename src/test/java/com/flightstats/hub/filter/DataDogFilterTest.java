package com.flightstats.hub.filter;

import com.flightstats.hub.app.HubProperties;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DataDogFilterTest {

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
        testsExpects.add(new String[]{"/channel/test/2016/latest", "/channel/channel/Y/latest"});
        testsExpects.add(new String[]{"channel/test/2016/03/latest", "/channel/channel/Y/M/latest"});
        testsExpects.add(new String[]{"/channel/test/2016/03/24/latest", "/channel/channel/Y/M/D/latest"});
        testsExpects.add(new String[]{"channel/test/2016/03/24/14/latest", "/channel/channel/Y/M/D/h/latest"});
        testsExpects.add(new String[]{"/channel/test/2016/03/24/14/31/latest", "/channel/channel/Y/M/D/h/m/latest"});
        testsExpects.add(new String[]{"channel/test/2016/03/24/14/31/25/latest", "/channel/channel/Y/M/D/h/m/s/latest"});

        for (String[] test : testsExpects) {
            assertEquals(test[1], filter.constructDeclaredpath(test[0], method));
        }
    }

    @Test
    public void testChanelWildcards() {
        DataDogRequestFilter filter = new DataDogRequestFilter();
        String method = "GET";
        List<String[]> testsExpects = new ArrayList<>();
        testsExpects.add(new String[]{"channel/test", "/channel/channel"});
        testsExpects.add(new String[]{"/channel/test/2016", "/channel/channel/Y"});
        testsExpects.add(new String[]{"channel/test/2016/03", "/channel/channel/Y/M"});
        testsExpects.add(new String[]{"/channel/test/2016/03/24", "/channel/channel/Y/M/D"});
        testsExpects.add(new String[]{"channel/test/2016/03/24/14", "/channel/channel/Y/M/D/h"});
        testsExpects.add(new String[]{"/channel/test/2016/03/24/14/31", "/channel/channel/Y/M/D/h/m"});
        testsExpects.add(new String[]{"channel/test/2016/03/24/14/31/25", "/channel/channel/Y/M/D/h/m/s"});

        for (String[] test : testsExpects) {
            assertEquals(test[1], filter.constructDeclaredpath(test[0], method));
        }
    }

    @Test
    public void testChannelTime() {
        DataDogRequestFilter filter = new DataDogRequestFilter();
        String method = "GET";
        List<String[]> testsExpects = new ArrayList<>();
        testsExpects.add(new String[]{"/channel/test/time", "/channel/channel/time"});
        testsExpects.add(new String[]{"channel/test/time/hour", "/channel/channel/time/hour"});
        testsExpects.add(new String[]{"/channel/test/time/minute", "/channel/channel/time/minute"});
        testsExpects.add(new String[]{"channel/test/time/second", "/channel/channel/time/second"});
        testsExpects.add(new String[]{"/channel/test/time/day", "/channel/channel/time/day"});

        for (String[] test : testsExpects) {
            assertEquals(test[1], filter.constructDeclaredpath(test[0], method));
        }
    }

    @Test
    public void TestChannelBulk() {
        DataDogRequestFilter filter = new DataDogRequestFilter();
        String method = "GET";
        List<String[]> testsExpects = new ArrayList<>();
        testsExpects.add(new String[]{"/channel/load_test_2/bulk", "/channel/channel/bulk"});
        testsExpects.add(new String[]{"channel/load_test_2/bulk", "/channel/channel/bulk"});

        for (String[] test : testsExpects) {
            assertEquals(test[1], filter.constructDeclaredpath(test[0], method));
        }
    }

    @Test
    public void testChannelEarliest() {
        DataDogRequestFilter filter = new DataDogRequestFilter();
        String method = "GET";
        List<String[]> testsExpects = new ArrayList<>();
        testsExpects.add(new String[]{"/channel/test/earliest", "/channel/channel/earliest"});

        for (String[] test : testsExpects) {
            assertEquals(test[1], filter.constructDeclaredpath(test[0], method));
        }
    }

    @Test
    public void testChannelBulk() {
        DataDogRequestFilter filter = new DataDogRequestFilter();
        String method = "GET";
        List<String[]> testsExpects = new ArrayList<>();
        testsExpects.add(new String[]{"channel/test/bulk", "/channel/channel/bulk"});
        testsExpects.add(new String[]{"/channel/test/bulk", "/channel/channel/bulk"});

        for (String[] test : testsExpects) {
            assertEquals(test[1], filter.constructDeclaredpath(test[0], method));
        }
    }

    @Test
    public void testChannelBatch() {
        DataDogRequestFilter filter = new DataDogRequestFilter();
        String method = "GET";
        List<String[]> testsExpects = new ArrayList<>();
        testsExpects.add(new String[]{"channel/test/batch", "/channel/channel/batch"});
        testsExpects.add(new String[]{"/channel/test/batch", "/channel/channel/batch"});

        for (String[] test : testsExpects) {
            assertEquals(test[1], filter.constructDeclaredpath(test[0], method));
        }
    }

    @Test
    public void testSpokePayloadPath() {
        DataDogRequestFilter filter = new DataDogRequestFilter();
        String method = "GET";
        List<String[]> testsExpects = new ArrayList<>();
        testsExpects.add(new String[]{"internal/spoke/payload/somechannel/somekey", "/internal/spoke/payload/path"});
        testsExpects.add(new String[]{"/internal/spoke/bulkKey/load_test_1", "/internal/spoke/bulkKey/C"});

        for (String[] test : testsExpects) {
            assertEquals(test[1], filter.constructDeclaredpath(test[0], method));
        }
    }

    @Test
    public void testInternalSpokeTimeTimePath() {
        DataDogRequestFilter filter = new DataDogRequestFilter();
        String method = "GET";
        List<String[]> testsExpects = new ArrayList<>();
        testsExpects.add(new String[]{"/internal/spoke/time/test/2016", "/internal/spoke/time/channel/Y"});
        testsExpects.add(new String[]{"internal/spoke/time/test/2016/03", "/internal/spoke/time/channel/Y/M"});
        testsExpects.add(new String[]{"/internal/spoke/time/test/2016/03/21", "/internal/spoke/time/channel/Y/M/D"});
        testsExpects.add(new String[]{"internal/spoke/time/test/2016/03/21/12", "/internal/spoke/time/channel/Y/M/D/h"});
        testsExpects.add(new String[]{"/internal/spoke/time/test/2016/03/21/12/31", "/internal/spoke/time/channel/Y/M/D/h/m"});
        testsExpects.add(new String[]{"internal/spoke/time/test/2016/03/21/12/31/15", "/internal/spoke/time/channel/Y/M/D/h/m/s"});
        testsExpects.add(new String[]{"/internal/spoke/time/test/2016/03/21/12/31/15/500", "/internal/spoke/time/channel/Y/M/D/h/m/s/ms"});

        for (String[] test : testsExpects) {
            assertEquals(test[1], filter.constructDeclaredpath(test[0], method));
        }
    }

    @Test
    public void testTags() {
        DataDogRequestFilter filter = new DataDogRequestFilter();
        String method = "GET";
        List<String[]> testsExpects = new ArrayList<>();
        testsExpects.add(new String[]{"tag", "/tag"});
        testsExpects.add(new String[]{"/tag/test", "/tag/tag"});
        testsExpects.add(new String[]{"tag/test/latest?stable=true", "/tag/tag/latest"});
        testsExpects.add(new String[]{"/tag/test/2016/04/01/17/59/54/656/KXo3du/previous/10000", "/tag/tag/Y/M/D/h/m/s/ms/hash/previous/count"});
        testsExpects.add(new String[]{"tag/test/2016/04/01/14/48/23/468/E9Yfkc/next/100", "/tag/tag/Y/M/D/h/m/s/ms/hash/next/count"});
        testsExpects.add(new String[]{"/tag/test/2016/03/30/12/43/33/430/y8t02V", "/tag/tag/Y/M/D/h/m/s/ms/hash"});
        testsExpects.add(new String[]{"/tag/replicated", "/tag/tag"});
        testsExpects.add(new String[]{"/tag/02732021592091769/time/hour", "/tag/tag/time/hour"});

        for (String[] test : testsExpects) {
            assertEquals(test[1], filter.constructDeclaredpath(test[0], method));
        }
    }

    @Test
    public void testInternalZookeeper() {
        DataDogRequestFilter filter = new DataDogRequestFilter();
        String method = "GET";
        List<String[]> testsExpects = new ArrayList<>();
        testsExpects.add(new String[]{"internal/zookeeper/", "/internal/zookeeper"});
        testsExpects.add(new String[]{"/internal/zookeeper/", "/internal/zookeeper"});
        testsExpects.add(new String[]{"internal/zookeeper/somepath", "/internal/zookeeper/path"});
        testsExpects.add(new String[]{"/internal/zookeeper/somepath", "/internal/zookeeper/path"});

        for (String[] test : testsExpects) {
            assertEquals(test[1], filter.constructDeclaredpath(test[0], method));
        }
    }

    @Test
    public void testS3Batch() {
        DataDogRequestFilter filter = new DataDogRequestFilter();
        String method = "GET";
        List<String[]> testsExpects = new ArrayList<>();
        testsExpects.add(new String[]{"internal/s3Batch/", "/internal/s3Batch"});
        testsExpects.add(new String[]{"/internal/s3Batch/", "/internal/s3Batch"});
        testsExpects.add(new String[]{"internal/s3Batch/someChannel", "/internal/s3Batch/C"});
        testsExpects.add(new String[]{"/internal/s3Batch/someChannel", "/internal/s3Batch/C"});
        testsExpects.add(new String[]{"/internal/s3Batch/from_dev_replicated", "/internal/s3Batch/C"});
        testsExpects.add(new String[]{"internal/s3Batch/load_test_3", "/internal/s3Batch/C"});

        for (String[] test : testsExpects) {
            assertEquals(test[1], filter.constructDeclaredpath(test[0], method));
        }
    }

    @Test
    public void testInternalEvents() {
        DataDogRequestFilter filter = new DataDogRequestFilter();
        String method = "GET";
        List<String[]> testsExpects = new ArrayList<>();
        testsExpects.add(new String[]{"internal/events/123", "/internal/events/id"});
        testsExpects.add(new String[]{"internal/events/Events_hub_ucs_dev_test_0_9590464073698968_K6FWuH", "/internal/events/id"});
        testsExpects.add(new String[]{"/internal/events/Events_hub_ucs_dev_test_0_5843572814483196_IUICgI", "/internal/events/id"});

        for (String[] test : testsExpects) {
            assertEquals(test[1], filter.constructDeclaredpath(test[0], method));
        }
    }

    @Test
    public void testInternalTime() {
        DataDogRequestFilter filter = new DataDogRequestFilter();
        String method = "GET";
        List<String[]> testsExpects = new ArrayList<>();
        testsExpects.add(new String[]{"internal/time/", "/internal/time"});
        testsExpects.add(new String[]{"internal/time/millis", "/internal/time/millis"});
        testsExpects.add(new String[]{"internal/time/remote", "/internal/time/remote"});
        testsExpects.add(new String[]{"internal/time/local", "/internal/time/local"});

        testsExpects.add(new String[]{"/internal/time/", "/internal/time"});
        testsExpects.add(new String[]{"/internal/time/millis", "/internal/time/millis"});
        testsExpects.add(new String[]{"/internal/time/remote", "/internal/time/remote"});
        testsExpects.add(new String[]{"/internal/time/local", "/internal/time/local"});

        for (String[] test : testsExpects) {
            assertEquals(test[1], filter.constructDeclaredpath(test[0], method));
        }
    }

    @Test
    public void testInternalWs() {
        DataDogRequestFilter filter = new DataDogRequestFilter();
        String method = "GET";
        List<String[]> testsExpects = new ArrayList<>();
        testsExpects.add(new String[]{"internal/ws/someId", "/internal/ws"});
        testsExpects.add(new String[]{"/internal/ws/someId", "/internal/ws"});

        for (String[] test : testsExpects) {
            assertEquals(test[1], filter.constructDeclaredpath(test[0], method));
        }
    }

    @Test
    public void testHealth() {
        DataDogRequestFilter filter = new DataDogRequestFilter();
        String method = "GET";
        List<String[]> testsExpects = new ArrayList<>();
        testsExpects.add(new String[]{"health/", "/health"});
        testsExpects.add(new String[]{"health/metrics", "/health/metrics"});
        testsExpects.add(new String[]{"health/metrics/trace", "/health/metrics/trace"});

        testsExpects.add(new String[]{"/health/", "/health"});
        testsExpects.add(new String[]{"/health/metrics", "/health/metrics"});
        testsExpects.add(new String[]{"/health/metrics/trace", "/health/metrics/trace"});

        for (String[] test : testsExpects) {
            assertEquals(test[1], filter.constructDeclaredpath(test[0], method));
        }
    }

    @Test
    public void testProvider() {
        DataDogRequestFilter filter = new DataDogRequestFilter();
        String method = "GET";
        List<String[]> testsExpects = new ArrayList<>();
        testsExpects.add(new String[]{"provider", "/provider"});
        testsExpects.add(new String[]{"/provider", "/provider"});

        for (String[] test : testsExpects) {
            assertEquals(test[1], filter.constructDeclaredpath(test[0], method));
        }
    }

    @Test
    public void testGroup() {
        DataDogRequestFilter filter = new DataDogRequestFilter();
        String method = "GET";
        List<String[]> testsExpects = new ArrayList<>();
        testsExpects.add(new String[]{"group", "/group"});
        testsExpects.add(new String[]{"group/testName", "/group/name"});
        testsExpects.add(new String[]{"group/Repl_hub_v2_dev_destination", "/group/name"});
        testsExpects.add(new String[]{"/group", "/group"});
        testsExpects.add(new String[]{"/group/testName", "/group/name"});
        testsExpects.add(new String[]{"/group/Repl_hub_v2_dev_destination", "/group/name"});

        for (String[] test : testsExpects) {
            assertEquals(test[1], filter.constructDeclaredpath(test[0], method));
        }
    }




}
