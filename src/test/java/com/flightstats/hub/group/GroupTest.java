package com.flightstats.hub.group;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GroupTest {

    private Group groupName;

    @Before
    public void setUp() throws Exception {
        groupName = Group.builder()
                .channelUrl("url").callbackUrl("end").build();
    }

    @Test
    public void testSimple() throws Exception {
        Group group = Group.fromJson(groupName.toJson());
        Assert.assertEquals("end", group.getCallbackUrl());
        Assert.assertEquals("url", group.getChannelUrl());
        Assert.assertNull(group.getName());
    }

    @Test
    public void testWithName() throws Exception {
        Group group = groupName.withName("wither");
        group = Group.fromJson(group.toJson());
        Assert.assertEquals("end", group.getCallbackUrl());
        Assert.assertEquals("url", group.getChannelUrl());
        Assert.assertEquals("wither", group.getName());
    }

    @Test(expected = NullPointerException.class)
    public void testNullEndpoint() throws Exception {
        Group.builder().channelUrl("url").build();
    }

    @Test(expected = NullPointerException.class)
    public void testNullChannelUrl() throws Exception {
        Group.builder().callbackUrl("end").build();
    }

}