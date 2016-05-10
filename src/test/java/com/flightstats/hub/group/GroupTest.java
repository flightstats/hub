package com.flightstats.hub.group;

import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.MinutePath;
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
    public void testJsonContentPath() {
        MinutePath key = new MinutePath();
        String json = "{\"callbackUrl\":\"end\",\"channelUrl\":\"url\"," +
                "\"startItem\":\"http://hub/channel/stuff/" + key.toUrl() +
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
        group = group.withDefaults(true);
        assertEquals(1L, (long) group.getParallelCalls());
        assertEquals("SINGLE", group.getBatch());
    }

    @Test
    public void testAllowedToChange() {
        Group hubA = Group.builder().name("name")
                .channelUrl("http://hubA/channel/name")
                .callbackUrl("url").build().withDefaults(false);
        Group hubB = Group.builder().name("name")
                .channelUrl("http://hubB/channel/name")
                .callbackUrl("url").build().withDefaults(false);

        assertTrue(hubA.allowedToChange(hubB));

        assertFalse(hubA.isChanged(hubB));

    }

    @Test
    public void testNotChanged() {
        Group hubA = Group.builder().name("name")
                .channelUrl("http://hubA/channel/name")
                .callbackUrl("url").build().withDefaults(false);

        Group hubC = Group.builder().name("name")
                .channelUrl("http://hubC/channel/nameC")
                .callbackUrl("url").build().withDefaults(false);

        assertFalse(hubA.allowedToChange(hubC));

        assertFalse(hubA.isChanged(hubC));

    }


}