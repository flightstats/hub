package com.flightstats.hub.service;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.exception.ConflictException;
import com.flightstats.hub.model.exception.InvalidRequestException;
import com.google.common.base.Strings;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateChannelValidatorTest {

    private ChannelService channelService;
    private CreateChannelValidator validator;

    @Before
    public void setUp() throws Exception {
        channelService = mock(ChannelService.class);
        validator = new CreateChannelValidator(channelService);
        when(channelService.channelExists(any(String.class))).thenReturn(false);
    }

    @Test
    public void testAllGood() throws Exception {
        validator.validate(ChannelConfiguration.builder().withName(Strings.repeat("A", 48)).build());
    }

    @Test(expected = InvalidRequestException.class)
    public void testTooLong() throws Exception {
        validator.validate(ChannelConfiguration.builder().withName(Strings.repeat("A", 49)).build());
    }

    @Test(expected = InvalidRequestException.class)
    public void testChannelNameNull() throws Exception {
        validator.validate(ChannelConfiguration.builder().withName(null).build());
    }

    @Test(expected = InvalidRequestException.class)
    public void testChannelNameEmpty() throws Exception {
        validator.validate(ChannelConfiguration.builder().withName("").build());
    }

    @Test(expected = InvalidRequestException.class)
    public void testChannelNameBlank() throws Exception {
        validator.validate(ChannelConfiguration.builder().withName("  ").build());
    }

    @Test(expected = ConflictException.class)
    public void testChannelExists() throws Exception {
        String channelName = "achannel";
        when(channelService.channelExists(channelName)).thenReturn(true);
        validator.validate(ChannelConfiguration.builder().withName(channelName).build());
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidSpaceCharacter() throws Exception {
        validator.validate(ChannelConfiguration.builder().withName("my chan").build());
    }

    @Test
    public void testInvalidUnderscore() throws Exception {
        validator.validate(ChannelConfiguration.builder().withName("my_chan").build());
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidHyphen() throws Exception {
        validator.validate(ChannelConfiguration.builder().withName("my-chan").build());
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidCharacter() throws Exception {
        validator.validate(ChannelConfiguration.builder().withName("my#chan").build());
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidContentSize() throws Exception {
        validator.validate(ChannelConfiguration.builder().withName("mychan").withContentKiloBytes(0).build());
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidRequestRate() throws Exception {
        validator.validate(ChannelConfiguration.builder().withName("mychan").withPeakRequestRate(0).build());
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidChannelTtl() throws Exception {
        validator.validate(ChannelConfiguration.builder().withName("mychan").withTtlDays(0).build());
    }

    @Test
    public void testDescription1024() throws Exception {
        validator.validate(ChannelConfiguration.builder().withName("desc").withDescription(Strings.repeat("A", 1024)).build());
    }

    @Test(expected = InvalidRequestException.class)
    public void testDescriptionTooBig() throws Exception {
        validator.validate(ChannelConfiguration.builder().withName("toobig").withDescription(Strings.repeat("A", 1025)).build());
    }

    @Test(expected = InvalidRequestException.class)
    public void testTagSpace() throws Exception {
        validator.validate(ChannelConfiguration.builder().withName("space").withTags(Arrays.asList("s p a c e")).build());
    }

    @Test(expected = InvalidRequestException.class)
    public void testTagUnderscore() throws Exception {
        validator.validate(ChannelConfiguration.builder().withName("underscore").withTags(Arrays.asList("under_score")).build());
    }

    @Test
    public void testTagValid() throws Exception {
        validator.validate(ChannelConfiguration.builder().withName("valid1").withTags(Arrays.asList("abcdefghijklmnopqrstuvwxyz")).build());
        validator.validate(ChannelConfiguration.builder().withName("valid2").withTags(Arrays.asList("ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789")).build());
    }

    @Test(expected = InvalidRequestException.class)
    public void testTagTooLong() throws Exception {
        validator.validate(ChannelConfiguration.builder().withName("tooLongTag")
                .withTags(Arrays.asList(Strings.repeat("A", 49))).build());
    }

    @Test(expected = InvalidRequestException.class)
    public void testTooManyTags() throws Exception {
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            tags.add("" + i);
        }
        validator.validate(ChannelConfiguration.builder().withName("tooManyTags")
                .withTags(tags).build());
    }
}
