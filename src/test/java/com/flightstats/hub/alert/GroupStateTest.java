package com.flightstats.hub.alert;

import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.webhook.GroupStatus;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

import static com.flightstats.hub.test.SparkUtil.get;
import static com.flightstats.hub.test.SparkUtil.stop;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GroupStateTest {

    private String hubAppUrl = "http://localhost:4567/";

    @AfterClass
    public static void tearDown() throws Exception {
        stop();
    }

    @Test
    public void testParse() throws IOException {
        URL resource = GroupStateTest.class.getResource("/group.json");
        String configString = IOUtils.toString(resource);
        get("/group/testParse", (req, res) -> configString);
        AlertConfig alertConfig = AlertConfig.builder()
                .channel("testParse")
                .hubDomain(hubAppUrl)
                .build();
        GroupStatus groupStatus = GroupState.getGroupStatus(alertConfig);
        assertEquals(new ContentKey(2015, 5, 28, 17, 6, 42, 376, "zYSn90"), groupStatus.getChannelLatest());
        assertEquals(new ContentKey(2015, 5, 28, 17, 0, 42, 376, "ABC"), groupStatus.getLastCompleted());
        assertEquals("http://hub/channel/provider", groupStatus.getGroup().getChannelUrl());
    }

    @Test
    public void testParseNoLatest() throws IOException {
        URL resource = GroupStateTest.class.getResource("/group-no-latest.json");
        String configString = IOUtils.toString(resource);
        get("/group/testParseNoLatest", (req, res) -> configString);
        AlertConfig alertConfig = AlertConfig.builder()
                .channel("testParseNoLatest")
                .hubDomain(hubAppUrl)
                .build();
        GroupStatus groupStatus = GroupState.getGroupStatus(alertConfig);
        assertNull(groupStatus.getChannelLatest());
        assertNull(groupStatus.getLastCompleted());
        assertEquals("http://hub/channel/provider", groupStatus.getGroup().getChannelUrl());
    }

    @Test
    public void testNoGroup() throws IOException {
        get("/group/testParseNoLatest", (req, res) -> {
            res.status(404);
            return "";
        });
        AlertConfig alertConfig = AlertConfig.builder()
                .channel("testNoGroup")
                .hubDomain(hubAppUrl)
                .build();
        GroupStatus groupStatus = GroupState.getGroupStatus(alertConfig);
        assertNull(groupStatus);
    }
}