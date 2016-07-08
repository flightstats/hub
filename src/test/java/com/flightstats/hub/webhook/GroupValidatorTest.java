package com.flightstats.hub.webhook;

import com.flightstats.hub.exception.InvalidRequestException;
import com.google.common.base.Strings;
import org.junit.Before;
import org.junit.Test;

public class GroupValidatorTest {

    private GroupValidator groupValidator;
    private Group group;

    @Before
    public void setUp() throws Exception {
        groupValidator = new GroupValidator();
        group = Group.builder()
                .callbackUrl("http://client/url")
                .channelUrl("http://hub/channel/channelName")
                .parallelCalls(1)
                .build();
    }

    @Test
    public void testName() throws Exception {
        group = group.withDefaults(true);
        groupValidator.validate(group.withName("aA9_-"));
    }

    @Test
    public void testNameLarge() throws Exception {
        group = group.withDefaults(true);
        groupValidator.validate(group.withName(Strings.repeat("B", 128)));
    }

    @Test(expected = InvalidRequestException.class)
    public void testNameSizeTooBig() throws Exception {
        groupValidator.validate(group.withName(Strings.repeat("B", 129)));
    }

    @Test(expected = InvalidRequestException.class)
    public void testZeroCalls() throws Exception {
        groupValidator.validate(group.withParallelCalls(0));
    }

    @Test(expected = InvalidRequestException.class)
    public void testNameChars() throws Exception {
        groupValidator.validate(group.withName("aA9:"));
    }

    @Test(expected = InvalidRequestException.class)
    public void testNonChannelUrl() throws Exception {
        groupValidator.validate(Group.builder()
                .callbackUrl("http:/client/url")
                .channelUrl("http:\\hub/channel/channelName")
                .parallelCalls(1)
                .name("nothing")
                .build());
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidCallbackUrl() throws Exception {
        groupValidator.validate(Group.builder()
                .callbackUrl("not a url")
                .channelUrl("http://hub/channel/channelName")
                .parallelCalls(1)
                .name("nothing")
                .build());
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidChannelUrl() throws Exception {
        groupValidator.validate(Group.builder()
                .callbackUrl("http:/client/url")
                .channelUrl("http://hub/channe/channelName")
                .parallelCalls(1)
                .name("testInvalidChannelUrl")
                .build());
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidBatch() throws Exception {
        group = group.withBatch("non").withName("blah");
        groupValidator.validate(group);
    }

    @Test
    public void testBatchLowerCase() throws Exception {
        group = group.withBatch("single").withName("blah");
        groupValidator.validate(group);
    }

}