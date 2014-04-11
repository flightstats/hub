package com.flightstats.hub.service;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.exception.AlreadyExistsException;
import com.flightstats.hub.model.exception.InvalidRequestException;
import com.google.common.base.Strings;
import org.junit.Before;
import org.junit.Test;

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

    @Test(expected = AlreadyExistsException.class)
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

}
