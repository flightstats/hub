package com.flightstats.hub.group;

import com.flightstats.hub.model.ContentKey;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class GroupTest {

    private Group group;

    @Before
    public void setUp() throws Exception {
        group = Group.builder()
                .channelUrl("url").callbackUrl("end").build();
    }

    @Test
    public void testSimple() throws Exception {
        Group group = Group.fromJson(this.group.toJson());
        assertEquals("end", group.getCallbackUrl());
        assertEquals("url", group.getChannelUrl());
        Assert.assertNull(group.getName());
    }

    @Test
    public void testWithName() throws Exception {
        Group group = this.group.withName("wither");
        group = Group.fromJson(group.toJson());
        assertEquals("end", group.getCallbackUrl());
        assertEquals("url", group.getChannelUrl());
        assertEquals("wither", group.getName());
    }

    @Test
    public void testFromJson() {
        System.out.println(group.toJson());
        Group cycled = Group.fromJson(group.toJson());
        assertEquals(group, cycled);
    }

    @Test
    public void testJsonStartItem() {
        ContentKey key = new ContentKey();
        String json = "{\"callbackUrl\":\"end\",\"channelUrl\":\"url\",\"startItem\":\"" +
                "http://hub/channel/stuff/" + key.toUrl() +
                "\"}";
        Group cycled = Group.fromJson(json);
        assertEquals(group, cycled);
        assertEquals(key, cycled.getStartingKey());
        String toJson = cycled.toJson();
        assertNotNull(toJson);
    }

    @Test
    public void testWithDefaults() {
        assertNull(group.getParallelCalls());
        assertNull(group.getBatch());
        group = group.withDefaults();
        assertEquals(1L, (long) group.getParallelCalls());
        assertEquals("SINGLE", group.getBatch());
    }


}