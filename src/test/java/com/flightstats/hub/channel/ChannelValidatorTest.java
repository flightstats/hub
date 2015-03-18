package com.flightstats.hub.channel;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.exception.ConflictException;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.model.ChannelConfig;
import com.google.common.base.Strings;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChannelValidatorTest {

    private ChannelService channelService;
    private ChannelValidator validator;

    @Before
    public void setUp() throws Exception {
        channelService = mock(ChannelService.class);
        validator = new ChannelValidator(channelService);
        when(channelService.channelExists(any(String.class))).thenReturn(false);
    }

    @Test
    public void testAllGood() throws Exception {
        validator.validate(ChannelConfig.builder().withName(Strings.repeat("A", 48)).build(), true);
    }

    @Test(expected = InvalidRequestException.class)
    public void testTooLong() throws Exception {
        validator.validate(ChannelConfig.builder().withName(Strings.repeat("A", 49)).build(), true);
    }

    @Test(expected = InvalidRequestException.class)
    public void testChannelNameNull() throws Exception {
        validator.validate(ChannelConfig.builder().withName(null).build(), true);
    }

    @Test(expected = InvalidRequestException.class)
    public void testChannelNameEmpty() throws Exception {
        validator.validate(ChannelConfig.builder().withName("").build(), true);
    }

    @Test(expected = InvalidRequestException.class)
    public void testChannelNameBlank() throws Exception {
        validator.validate(ChannelConfig.builder().withName("  ").build(), true);
    }

    @Test(expected = ConflictException.class)
    public void testChannelExists() throws Exception {
        String channelName = "achannel";
        when(channelService.channelExists(channelName)).thenReturn(true);
        validator.validate(ChannelConfig.builder().withName(channelName).build(), true);
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidSpaceCharacter() throws Exception {
        validator.validate(ChannelConfig.builder().withName("my chan").build(), true);
    }

    @Test
    public void testInvalidUnderscore() throws Exception {
        validator.validate(ChannelConfig.builder().withName("my_chan").build(), true);
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidHyphen() throws Exception {
        validator.validate(ChannelConfig.builder().withName("my-chan").build(), true);
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidCharacter() throws Exception {
        validator.validate(ChannelConfig.builder().withName("my#chan").build(), true);
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidChannelTtl() throws Exception {
        validator.validate(ChannelConfig.builder().withName("mychan").withTtlDays(0).build(), true);
    }

    @Test
    public void testDescription1024() throws Exception {
        validator.validate(ChannelConfig.builder().withName("desc").withDescription(Strings.repeat("A", 1024)).build(), true);
    }

    @Test(expected = InvalidRequestException.class)
    public void testDescriptionTooBig() throws Exception {
        validator.validate(ChannelConfig.builder().withName("toobig").withDescription(Strings.repeat("A", 1025)).build(), true);
    }

    @Test(expected = InvalidRequestException.class)
    public void testTagSpace() throws Exception {
        validator.validate(ChannelConfig.builder().withName("space").withTags(Arrays.asList("s p a c e")).build(), true);
    }

    @Test(expected = InvalidRequestException.class)
    public void testTagUnderscore() throws Exception {
        validator.validate(ChannelConfig.builder().withName("underscore").withTags(Arrays.asList("under_score")).build(), true);
    }

    @Test
    public void testTagValid() throws Exception {
        validator.validate(ChannelConfig.builder().withName("valid1").withTags(Arrays.asList("abcdefghijklmnopqrstuvwxyz")).build(), true);
        validator.validate(ChannelConfig.builder().withName("valid2").withTags(Arrays.asList("ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789")).build(), true);
    }

    @Test(expected = InvalidRequestException.class)
    public void testTagTooLong() throws Exception {
        validator.validate(ChannelConfig.builder().withName("tooLongTag")
                .withTags(Arrays.asList(Strings.repeat("A", 49))).build(), true);
    }

    @Test(expected = InvalidRequestException.class)
    public void testTooManyTags() throws Exception {
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            tags.add("" + i);
        }
        validator.validate(ChannelConfig.builder().withName("tooManyTags")
                .withTags(tags).build(), true);
    }

    @Test
    public void testAllowColonAndDash() throws Exception {
        validator.validate(ChannelConfig.builder().withName("colon").withTags(Arrays.asList("a:b")).build(), true);
        validator.validate(ChannelConfig.builder().withName("dash").withTags(Arrays.asList("a-b")).build(), true);
        validator.validate(ChannelConfig.builder().withName("colondash").withTags(Arrays.asList("a-b:c")).build(), true);
    }
}
