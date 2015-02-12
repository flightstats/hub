package com.flightstats.hub.group;

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
                .build();
    }

    @Test
    public void testName() throws Exception {
        groupValidator.validate(group.withName("aA9"));
    }

    @Test
    public void testNameLarge() throws Exception {
        groupValidator.validate(group.withName(Strings.repeat("B", 48)));
    }

    @Test(expected = InvalidRequestException.class)
    public void testNameSizeTooBig() throws Exception {
        groupValidator.validate(group.withName(Strings.repeat("B", 49)));
    }

    @Test(expected = InvalidRequestException.class)
    public void testNameChars() throws Exception {
        groupValidator.validate(group.withName("aA9-"));
    }

    @Test(expected = InvalidRequestException.class)
    public void testNonChannelUrl() throws Exception {
        groupValidator.validate(Group.builder()
                .callbackUrl("http:/client/url")
                .channelUrl("http:\\hub/channel/channelName")
                .name("nothing")
                .build());
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidCallbackUrl() throws Exception {
        groupValidator.validate(Group.builder()
                .callbackUrl("not a url")
                .channelUrl("http://hub/channel/channelName")
                .name("nothing")
                .build());
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidChannelUrl() throws Exception {
        groupValidator.validate(Group.builder()
                .callbackUrl("http:/client/url")
                .channelUrl("http://hub/channe/channelName")
                .name("testInvalidChannelUrl")
                .build());
    }
}